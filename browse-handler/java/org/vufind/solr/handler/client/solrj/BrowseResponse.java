package org.vufind.solr.handler.client.solrj;

import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.vufind.solr.handler.MatchTypeResponse.MatchType;

/**
 * Holds the response from BrowseRequest.
 *
 * Based somewhat on {@link QueryResponse}.
 *
 * @author tod
 *
 */
public class BrowseResponse extends SolrResponseBase
{
    // Direct pointers to known types
    private NamedList<String> _header = null;
    private Map<String,Object> _browse = null;
    // match type will have been converted from the Enum MatchType to String
    private String _matchType = null;

    // utility variable used for automatic binding -- it should not be serialized
    private transient final SolrClient solrClient;
    /**
     * Utility constructor to set the solrServer and namedList
     */
    public BrowseResponse(NamedList<Object> res, SolrClient solrClient)
    {
        this.setResponse(res);
        this.solrClient = solrClient;
    }

    public BrowseResponse(SolrClient solrClient)
    {
        this.solrClient = solrClient;
    }

    @Override
    public void setResponse(NamedList<Object> res)
    {
        super.setResponse(res);

        // Look for known things
        for (int i=0; i<res.size(); i++) {
            String n = res.getName(i);
            if ("responseHeader".equals(n)) {
                _header = (NamedList<String>) res.getVal(i);
            } else if ("Browse".equals(n)) {
                _browse = (Map<String,Object>) res.getVal(i);
            }
        }
        _matchType = (String) _browse.get("matchType");
    }

    public NamedList<String> getHeader()
    {
        return _header;
    }

    public Map<String,Object> getBrowse()
    {
        return _browse;
    }

    public String getMatchType()
    {
        return _matchType;
    }
}
