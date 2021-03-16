package datawave.query.planner;

import com.google.common.collect.Multimap;
import datawave.data.type.Type;
import datawave.query.CloseableIterable;
import datawave.query.config.FieldIndexHole;
import datawave.query.config.ShardQueryConfiguration;
import datawave.query.config.ValueIndexHole;
import datawave.query.exceptions.DatawaveFatalQueryException;
import datawave.query.exceptions.DatawaveQueryException;
import datawave.query.exceptions.NoResultsException;
import datawave.query.jexl.visitors.FetchDataTypesVisitor;
import datawave.query.planner.pushdown.rules.PushDownRule;
import datawave.query.tables.ScannerFactory;
import datawave.query.util.IndexedDatesValue;
import datawave.query.util.MetadataHelper;
import datawave.query.util.YearMonthDay;
import datawave.util.time.DateHelper;
import datawave.webservice.common.logging.ThreadConfigurableLogger;
import datawave.webservice.query.Query;
import datawave.webservice.query.configuration.GenericQueryConfiguration;
import datawave.webservice.query.configuration.QueryData;
import datawave.webservice.query.exception.DatawaveErrorCode;
import datawave.webservice.query.exception.PreConditionFailedQueryException;
import org.apache.accumulo.core.client.TableNotFoundException;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.accumulo.core.metadata.schema.MetadataSchema;
import org.apache.commons.jexl2.parser.ASTJexlScript;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

public class FederatedQueryPlanner extends DefaultQueryPlanner {
    
    private static final Logger log = ThreadConfigurableLogger.getLogger(FederatedQueryPlanner.class);
    
    public FederatedQueryPlanner() {
        super();
    }
    
    @Override
    public FederatedQueryDataIterable process(GenericQueryConfiguration config, String query, Query settings, ScannerFactory scannerFactory)
                    throws DatawaveQueryException {
        
        FederatedQueryDataIterable returnQueryData = new FederatedQueryDataIterable();
        Date originalEndDate = config.getEndDate();
        Date originalStartDate = config.getBeginDate();
        TreeSet<YearMonthDay> holeDates;
        MetadataHelper metadataHelper = getMetadataHelper();
        
        final QueryData queryData = new QueryData();
        CloseableIterable<QueryData> results;
        
        if (config instanceof ShardQueryConfiguration) {
            ASTJexlScript queryTree = null;
            try {
                queryTree = updateQueryTree(scannerFactory, metadataHelper, dateIndexHelper, (ShardQueryConfiguration) config, query, queryData, settings);
            } catch (StackOverflowError e) {
                if (log.isTraceEnabled()) {
                    log.trace("Stack trace for overflow " + e);
                }
                PreConditionFailedQueryException qe = new PreConditionFailedQueryException(DatawaveErrorCode.QUERY_DEPTH_OR_TERM_THRESHOLD_EXCEEDED, e);
                log.warn(qe);
                throw new DatawaveFatalQueryException(qe);
            } catch (NoResultsException e) {
                if (log.isTraceEnabled()) {
                    log.trace("Definitively determined that no results exist from the indexes");
                }
                
            }
            
            Multimap<String,Type<?>> fieldToDatatypeMap = FetchDataTypesVisitor.fetchDataTypes(metadataHelper,
                            ((ShardQueryConfiguration) config).getDatatypeFilter(), queryTree, false);
            
            try {
                calculateFieldIndexHoles(metadataHelper, fieldToDatatypeMap, (ShardQueryConfiguration) config);
            } catch (TableNotFoundException e) {
                log.error("metadata table was not found " + e.getMessage());
            }
            
            List<FieldIndexHole> fieldIndexHoles = ((ShardQueryConfiguration) config).getFieldIndexHoles();
            // if (log.isDebugEnabled() && log.getLevel().equals(Level.DEBUG))
            checkForErrorsInFieldIndexHoles((ShardQueryConfiguration) config);
            List<ValueIndexHole> valueIndexHoles = ((ShardQueryConfiguration) config).getValueIndexHoles();
            holeDates = generateStartAndEndDates((ShardQueryConfiguration) config);
            if ((valueIndexHoles == null && fieldIndexHoles == null) || (valueIndexHoles.size() == 0 && fieldIndexHoles.size() == 0) || holeDates.size() == 0) {
                results = super.process(config, query, settings, scannerFactory);
                returnQueryData.addDelegate(results);
                return returnQueryData;
            }
            
            Boolean firstIteration = true;
            Date startDate, endDate;
            
            for (Iterator<YearMonthDay> it = holeDates.iterator(); it.hasNext();) {
                if (firstIteration) {
                    firstIteration = false;
                    startDate = originalStartDate;
                    if (it.hasNext()) {
                        endDate = DateHelper.parse(it.next().getYyyymmdd());
                    } else
                        endDate = originalEndDate;
                } else {
                    startDate = DateHelper.parse(it.next().getYyyymmdd());
                    if (it.hasNext())
                        endDate = DateHelper.parse(it.next().getYyyymmdd());
                    else {
                        endDate = originalEndDate;
                    }
                }
                
                results = getQueryData((ShardQueryConfiguration) config, query, settings, scannerFactory, startDate, endDate);
                returnQueryData.addDelegate(results);
                
            }
            
        }
        
        return returnQueryData;
    }
    
    /*
     * This function removes improperly calculate field index holes and will be removed as soon as all debugging in ticket #825 is complete. I need to see that
     * the test don't pass because of bad index holes.
     */
    
    private void checkForErrorsInFieldIndexHoles(ShardQueryConfiguration config) {
        ArrayList<FieldIndexHole> removalList = new ArrayList<>();
        for (FieldIndexHole fieldIndexHole : config.getFieldIndexHoles()) {
            if (fieldIndexHole.getStartDate().compareTo(fieldIndexHole.getEndDate()) > 0) {
                log.error("There was a problem calculating the FieldIndexHole " + fieldIndexHole);
                log.error("End date in feild Index hole can't come before start date.");
                removalList.add(fieldIndexHole);
                log.info("Invalid field index hole removed " + fieldIndexHole);
            }
        }
        config.getFieldIndexHoles().removeAll(removalList);
    }
    
    private CloseableIterable<QueryData> getQueryData(ShardQueryConfiguration config, String query, Query settings, ScannerFactory scannerFactory,
                    Date startDate, Date endDate) throws DatawaveQueryException {
        log.debug("getQueryData in the FederatedQueryPlanner is called ");
        CloseableIterable<QueryData> queryData;
        ShardQueryConfiguration tempConfig = new ShardQueryConfiguration(config);
        tempConfig.setBeginDate(startDate);
        tempConfig.setEndDate(endDate);
        // TODO: I think it is unnecessary to clone the DefaultQueryPlanner but I think it was requested.
        DefaultQueryPlanner tempPlanner = new DefaultQueryPlanner(this);
        queryData = tempPlanner.process(tempConfig, query, settings, scannerFactory);
        return queryData;
    }
    
    private TreeSet<YearMonthDay> generateStartAndEndDates(ShardQueryConfiguration configuration) {
        
        String startDate = DateHelper.format(configuration.getBeginDate().getTime());
        String endDate = DateHelper.format(configuration.getEndDate().getTime());
        
        YearMonthDay.Bounds bounds = new YearMonthDay.Bounds(startDate, false, endDate, false);
        
        TreeSet<YearMonthDay> queryDates = new TreeSet<>();
        for (ValueIndexHole valueIndexHole : configuration.getValueIndexHoles()) {
            addDatesToSet(bounds, queryDates, valueIndexHole.getStartDate());
            addDatesToSet(bounds, queryDates, valueIndexHole.getEndDate());
        }
        
        for (FieldIndexHole fieldIndexHole : configuration.getFieldIndexHoles()) {
            // TODO remove comparison below. This may be wrong.
            if (fieldIndexHole.getStartDate().compareTo(fieldIndexHole.getEndDate()) == 0) {
                addDatesToSet(bounds, queryDates, fieldIndexHole.getStartDate());
                addDatesToSet(bounds, queryDates, fieldIndexHole.getEndDate());
            }
        }
        
        return queryDates;
    }
    
    private void addDatesToSet(YearMonthDay.Bounds bounds, TreeSet<YearMonthDay> queryDates, String strDate) {
        if (bounds.withinBounds(strDate))
            queryDates.add(new YearMonthDay(strDate));
    }
    
    /**
     * Calculate the FieldIndexHoles and add them to the ShardedQueryConfiguaration
     *
     * @param metadataHelper
     * @param fieldToDatatypeMap
     * @param config
     */
    public void calculateFieldIndexHoles(MetadataHelper metadataHelper, Multimap<String,Type<?>> fieldToDatatypeMap, ShardQueryConfiguration config)
                    throws TableNotFoundException {
        
        IndexedDatesValue indexedDates;
        String startDate = DateHelper.format(config.getBeginDate().getTime());
        String endDate = DateHelper.format(config.getEndDate().getTime());
        String holeStart = startDate;
        String lastHoleEndate = null;
        YearMonthDay.Bounds bounds = new YearMonthDay.Bounds(startDate, false, endDate, false);
        FieldIndexHole newHole = null;
        boolean firstHole = true;
        boolean foundHolesInDateBounds;
        String previousDay, nextDay;
        log.debug("startDate is: " + startDate + " and endDate is " + endDate);
        
        for (String field : fieldToDatatypeMap.keySet()) {
            indexedDates = metadataHelper.getIndexDates(field, config.getDatatypeFilter());
            if (indexedDates != null && indexedDates.getIndexedDatesSet().size() > 0) {
                for (YearMonthDay entry : indexedDates.getIndexedDatesSet()) {
                    // Only create a hole if the indexed field are within date bounds
                    if (bounds.withinBounds(entry)) {
                        foundHolesInDateBounds = true;
                        if (firstHole && holeStart.compareTo(entry.getYyyymmdd()) < 0) {
                            // create the FieldIndexHole for the dates the field was not indexed before the first
                            // time in the date range that it was indexed.
                            FieldIndexHole firstIndexHole = new FieldIndexHole(field, startDate);
                            previousDay = previousDay(entry.getYyyymmdd());
                            nextDay = nextDay(entry.getYyyymmdd());
                            log.debug("The date in the entry is: " + entry.getYyyymmdd());
                            log.debug("The previous day is: " + previousDay);
                            log.debug("The next day is: " + nextDay);
                            firstIndexHole.setEndDate(previousDay);
                            addFieldIndexHoleToConfig(config, firstIndexHole);
                            holeStart = nextDay(entry.getYyyymmdd());
                            firstHole = false;
                        }
                        
                        // The end date of the last hole processed depends on the next date the field was indexed
                        if (newHole != null) {
                            lastHoleEndate = previousDay(entry.getYyyymmdd());
                            newHole.setEndDate(lastHoleEndate);
                        } else {
                            lastHoleEndate = nextDay(entry.getYyyymmdd());
                            if (lastHoleEndate.compareTo(endDate) > 0)
                                lastHoleEndate = endDate;
                        }
                        
                        /*
                         * If the start of the next potential hole is the same as the date indexed field there is no need to start creating and index hole
                         * starting on that date so increment the holeStart and find the next indexed date.
                         */
                        if (holeStart.equals(entry.getYyyymmdd())) {
                            holeStart = nextDay(entry.getYyyymmdd());
                            continue;
                        }
                        
                        // At this point, create a new FieldIndexHole
                        if (bounds.withinBounds(holeStart)) {
                            newHole = new FieldIndexHole();
                            newHole.setFieldName(field);
                            newHole.setStartDate(holeStart);
                            // you have to see next date the the field was indexed in the next iteration
                            // before you can set the end date. That may get done outside the loop on line 1698
                            // or on 1669 with the previous day of the next date the field was indexed
                            addFieldIndexHoleToConfig(config, newHole);
                        }
                    }
                    holeStart = nextDay(entry.getYyyymmdd());
                }
                
                holeStart = startDate;
                foundHolesInDateBounds = false;
                
            } else {
                newHole = null;
                continue;
                
            }
            
            if (newHole != null)
                newHole.setEndDate(lastHoleEndate);
            
            if (foundHolesInDateBounds && bounds.withinBounds(holeStart)) {
                FieldIndexHole trailingHole = new FieldIndexHole(field, new String[] {holeStart, endDate});
                addFieldIndexHoleToConfig(config, trailingHole);
            } else
            
            if (foundHolesInDateBounds) {
                FieldIndexHole trailingHole = new FieldIndexHole(field, new String[] {startDate, endDate});
                addFieldIndexHoleToConfig(config, trailingHole);
            }
            
            if (!config.getFieldIndexHoles().isEmpty()) {
                log.debug("Found Field index holes for field: " + field + " within date bounds");
                for (FieldIndexHole hole : config.getFieldIndexHoles()) {
                    log.debug(hole.toString());
                }
            } else {
                log.debug("No fieldHoles created.");
            }
            
        }
        
    }
    
    private void addFieldIndexHoleToConfig(ShardQueryConfiguration config, FieldIndexHole fieldIndexHole) {
        config.addFieldIndexHole(fieldIndexHole);
    }
    
    private static String previousDay(String day) {
        return YearMonthDay.previousDay(day).getYyyymmdd();
    }
    
    private static String nextDay(String day) {
        return YearMonthDay.nextDay(day).getYyyymmdd();
    }
    
}
