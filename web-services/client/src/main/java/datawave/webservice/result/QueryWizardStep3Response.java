package datawave.webservice.result;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import datawave.webservice.HtmlProvider;
import datawave.webservice.query.result.logic.QueryLogicDescription;

import org.apache.commons.lang.StringUtils;

@XmlRootElement(name = "QueryWizardStep3")
@XmlAccessorType(XmlAccessType.NONE)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class QueryWizardStep3Response extends BaseResponse implements HtmlProvider {
    
    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Query Plan", EMPTY = "";
    @XmlElement(name = "plan")
    private String plan = "";
    @XmlElement(name = "queryId")
    private String queryId = "";
    
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }
    
    public void setQueryPlan(String theplan) {
        this.plan = theplan;
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
        builder.append("<H2>The query plan: " + plan + "</H2>");
        builder.append("<br/>");
        builder.append("<H2>Results</H2>");
        builder.append("<br/><br/>");
        builder.append("<FORM id=\"queryform\" action=\"/DataWave/Query/" + queryId
                        + "/next\"  method=\"get\" target=\"_self\" enctype=\"application/x-www-form-urlencoded\">");
        builder.append("<center><input type=\"submit\" value=\"Next\" align=\"left\" width=\"50\" /></center>");
        
        builder.append("</FORM>");
        
        return builder.toString();
    }
    
}
