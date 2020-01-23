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
import datawave.webservice.query.result.metadata.MetadataFieldBase;

@XmlRootElement(name = "QueryWizardNextResult")
@XmlAccessorType(XmlAccessType.NONE)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class QueryWizardResultResponse extends BaseResponse implements HtmlProvider {
    
    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Query Result", EMPTY = "";
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
        builder.append("<H1>DataWave Query Plan</H1>");
        builder.append("<br/>");
        builder.append("<br/>");
        builder.append("<br/>");
        builder.append("<H2>Results</H2>");
        builder.append("<br/><br/>");
        builder.append("<table>");
        if (response instanceof DefaultEventQueryResponse) {
            DefaultEventQueryResponse tempResponse = (DefaultEventQueryResponse) response;
            builder.append("<tr><th>Name</th><th>Value</th><th>Visibility</th><th>Typed Value</th></tr>");
            for (EventBase event : tempResponse.getEvents()) {
                for (Object field : event.getFields()) {
                    if (field instanceof DefaultField) {
                        DefaultField defaultField = (DefaultField) field;
                        builder.append("<tr>");
                        putTableCell(builder, defaultField.getName());
                        putTableCell(builder, defaultField.getValueString());
                        putTableCell(builder, defaultField.getColumnVisibility());
                        putTableCell(builder, defaultField.getTypedValue().toString());
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
            for (MetadataFieldBase field : tempResponse.getFields()) {
                builder.append(field.toString());
                builder.append("<br/>");
            }
            
        }
        
        builder.append("</table>");
        
        builder.append("<FORM id=\"queryform\" action=\"/DataWave/Query/" + queryId
                        + "/showQueryWizardResults\"  method=\"get\" target=\"_self\" enctype=\"application/x-www-form-urlencoded\">");
        builder.append("<center><input type=\"submit\" value=\"Next\" align=\"left\" width=\"50\" /></center>");
        
        builder.append("</FORM>");
        
        return builder.toString();
    }
    
}
