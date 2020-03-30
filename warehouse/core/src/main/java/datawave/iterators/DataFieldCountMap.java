package datawave.iterators;

import datawave.data.MetadataCardinalityCounts;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Combiner;
import datawave.data.ColumnFamilyConstants;
import datawave.data.type.Type;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class DataFieldCountMap extends Combiner implements Serializable {
    
    private static final Logger log = Logger.getLogger(DataFieldCountMap.class);
    
    private ArrayList<Integer> ingestCountArray = new ArrayList<>();
    private byte[] startKey = null;
    
    /**
     * Reduces a list of Values into a single Value.
     *
     * @param key
     *            The most recent version of the Key being reduced.
     *
     * @param iterator
     *            An iterator over the Values for different versions of the key.
     *
     * @return The combined Value.
     */
    @Override
    public Value reduce(Key key, Iterator<Value> iterator) {
        MetadataCardinalityCounts counts = null;
        Value singletonValue = null;
        
        while (iterator.hasNext()) {
            Value value = iterator.next();
            try {
                MetadataCardinalityCounts newCounts = new MetadataCardinalityCounts(key, value);
                if (counts == null) {
                    counts = newCounts;
                    singletonValue = value;
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("Merging " + counts + " with " + newCounts);
                    }
                    counts.merge(newCounts);
                    if (log.isTraceEnabled()) {
                        log.trace("Resulted in " + counts);
                    }
                    singletonValue = null;
                }
            } catch (Exception e) {
                log.error("Unable to decode counts from " + key + " / " + value);
            }
        }
        
        if (singletonValue != null) {
            return singletonValue;
        } else if (counts != null) {
            return counts.getValue();
        } else {
            return new Value();
        }
    }
    
}
