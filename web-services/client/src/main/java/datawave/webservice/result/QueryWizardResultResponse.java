package datawave.webservice.result;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import datawave.webservice.HtmlProvider;
import datawave.webservice.query.result.edge.EdgeBase;
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
    
    @Override
    public String getMainContent() {
        StringBuilder builder = new StringBuilder();
        builder.append("<H1>DataWave Query Plan</H1>");
        builder.append("<br/>");
        builder.append("<br/>");
        builder.append("<br/>");
        builder.append("<H2>Results</H2>");
        builder.append("<br/><br/>");
        if (response instanceof DefaultEventQueryResponse) {
            DefaultEventQueryResponse tempResponse = (DefaultEventQueryResponse) response;
            for (EventBase event : tempResponse.getEvents()) {
                for (Object field : event.getFields()) {
                    builder.append(field.toString());
                    builder.append("<br/>");
                }
                
            }
        } else if (response instanceof DefaultEdgeQueryResponse) {
            DefaultEdgeQueryResponse tempResponse = (DefaultEdgeQueryResponse) response;
            for (EdgeBase edge : tempResponse.getEdges()) {
                builder.append(edge.toString());
                builder.append("<br/>");
            }
        } else if (response instanceof DefaultMetadataQueryResponse) {
            DefaultMetadataQueryResponse tempResponse = (DefaultMetadataQueryResponse) response;
            for (MetadataFieldBase field : tempResponse.getFields()) {
                builder.append(field.toString());
                builder.append("<br/>");
            }
            
        }
        
        builder.append("<FORM id=\"queryform\" action=\"/DataWave/Query/" + queryId
                        + "/showQueryWizardResults\"  method=\"get\" target=\"_self\" enctype=\"application/x-www-form-urlencoded\">");
        builder.append("<center><input type=\"submit\" value=\"Next\" align=\"left\" width=\"50\" /></center>");
        
        builder.append("</FORM>");
        
        return builder.toString();
    }
    
}
