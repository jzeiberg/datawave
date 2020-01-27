package datawave.webservice.result;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import datawave.webservice.HtmlProvider;
import datawave.webservice.query.result.edge.EdgeBase;
import datawave.webservice.query.result.event.DefaultField;
import datawave.webservice.query.result.event.EventBase;
import datawave.webservice.query.result.istat.FieldStat;
import datawave.webservice.query.result.istat.IndexStatsResponse;
import datawave.webservice.query.result.metadata.MetadataFieldBase;

@XmlRootElement(name = "QueryWizardNextResult")
@XmlAccessorType(XmlAccessType.NONE)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class QueryWizardResultResponse extends BaseResponse implements HtmlProvider {
    
    private static final long serialVersionUID = 1L;
    private static final String TITLE = "DataWave Query Results", EMPTY = "";
    @XmlElement(name = "queryId")
    private String queryId = "";
    @XmlElement
    private BaseQueryResponse response = null;
    
    public void setResponse(BaseQueryResponse response) {
        this.response = response;
    }
    
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
    @Override
    public String getTitle() {
        return TITLE;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see datawave.webservice.HtmlProvider#getPageHeader()
     */
    @Override
    public String getPageHeader() {
        return getTitle();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see datawave.webservice.HtmlProvider#getHeadContent()
     */
    @Override
    public String getHeadContent() {
        return EMPTY;
    }
    
    private void putTableCell(StringBuilder builder, String cellValue) {
        builder.append("<td>\n");
        builder.append(cellValue);
        builder.append("</td>\n");
    }
    
    @Override
    public String getMainContent() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("<br/><br/>");
        if (response == null || !response.getHasResults()) {
            builder.append("<H2>There aren't anymore results</H2>");
            return builder.toString();
        }
        builder.append("<table>");
        if (response instanceof DefaultEventQueryResponse) {
            DefaultEventQueryResponse tempResponse = (DefaultEventQueryResponse) response;
            builder.append("<tr><th>RowID</th><th>Data Type</th><th>Field Name</th><th>Field Value</th></tr>");
            String rowId = "";
            String dataType = "";
            for (EventBase event : tempResponse.getEvents()) {
                rowId = event.getMetadata().getRow();
                dataType = event.getMetadata().getDataType();
                for (Object field : event.getFields()) {
                    if (field instanceof DefaultField) {
                        builder.append("<tr>");
                        DefaultField defaultField = (DefaultField) field;
                        putTableCell(builder, rowId);
                        putTableCell(builder, dataType);
                        putTableCell(builder, defaultField.getName());
                        if (defaultField.getValueString().length() > 300)
                            putTableCell(builder, defaultField.getValueString().substring(0, 299));
                        else
                            putTableCell(builder, defaultField.getValueString());
                        
                        builder.append("</tr>");
                        
                    }
                }
                
            }
        } else if (response instanceof DefaultEdgeQueryResponse) {
            DefaultEdgeQueryResponse tempResponse = (DefaultEdgeQueryResponse) response;
            builder.append("<tr><th>Source</th><th>Sink</th><th>Edge Type</th><th>Activity Date</th><th>Edge Relationship</th></tr>");
            for (EdgeBase edge : tempResponse.getEdges()) {
                builder.append("<tr>");
                putTableCell(builder, edge.getSource());
                putTableCell(builder, edge.getSink());
                putTableCell(builder, edge.getEdgeType());
                putTableCell(builder, edge.getActivityDate());
                putTableCell(builder, edge.getEdgeRelationship());
                builder.append("</tr>");
            }
        } else if (response instanceof DefaultMetadataQueryResponse) {
            DefaultMetadataQueryResponse tempResponse = (DefaultMetadataQueryResponse) response;
            builder.append("<tr><th>Field Name</th><th>Internal Field Name</th><th>Data Type</th><th>Last Updated</th><th>Index only</th></tr>");
            for (MetadataFieldBase field : tempResponse.getFields()) {
                builder.append("<tr>");
                putTableCell(builder, field.getFieldName());
                putTableCell(builder, field.getInternalFieldName());
                putTableCell(builder, field.getDataType());
                putTableCell(builder, field.getLastUpdated());
                putTableCell(builder, field.isIndexOnly().toString());
                builder.append("</tr>");
            }
        } else if (response instanceof IndexStatsResponse) {
            IndexStatsResponse tempResponse = (IndexStatsResponse) response;
            builder.append("<tr><th>Field Name</th><th>Observed</th><th>Selectivity</th><th>Unique</th></tr>");
            for (FieldStat stat : tempResponse.getFieldStats()) {
                builder.append("<tr>");
                putTableCell(builder, stat.field);
                putTableCell(builder, String.valueOf(stat.observed));
                putTableCell(builder, String.valueOf(stat.selectivity));
                putTableCell(builder, String.valueOf(stat.unique));
            }
        }
        
        builder.append("</table>");
        builder.append("<FORM id=\"queryform\" action=\"/DataWave/BasicQuery/" + queryId
                        + "/showQueryWizardResults\"  method=\"get\" target=\"_self\" enctype=\"application/x-www-form-urlencoded\">");
        builder.append("<center><input type=\"submit\" value=\"Next\" align=\"left\" width=\"50\" /></center>");
        
        builder.append("</FORM>");
        
        return builder.toString();
    }
    
}
