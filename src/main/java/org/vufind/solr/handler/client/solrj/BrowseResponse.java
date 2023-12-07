package org.vufind.solr.handler.client.solrj;

import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.apache.solr.common.util.NamedList;

/**
 * Holds the response from BrowseRequest.
 * <p>
 * The {@code getResponse()} method returns a {@code NamedList<Object>} of
 * only two elements: {@code responseHeader} and {@code Browse}.
 * <p>
 * {@code Browse} is a {@code Map<String,Object>} with keys
 * {@literal startRow}, {@literal offset}, {@literal matchType},
 * {@literal totalCount}, and {@literal items}.
 * The value of {@literal items} is a {@code BrowseList}.
 * <p>
 * Based somewhat on {@link QueryResponse}.
 *
 * @author Tod Olson <tod@uchicago.edu>
 *
 */
@SuppressWarnings("serial")
public class BrowseResponse extends SolrResponseBase
{
    // Direct pointers to known types
    private NamedList<String> _header = null;
    private Map<String,Object> _browse = null;
    // match type will have been converted from the Enum MatchType to String
    private String _matchType = null;

    // utility variable used for automatic binding -- it should not be serialized
    @SuppressWarnings("unused")
    private transient final SolrClient solrClient;

    /**
     * Casts an object to a NamedList of Strings.
     *
     * The only reason this method exists is to isolate the SuppressWarnings decoration.
     *
     * @param o
     * @return
     */
    @SuppressWarnings("unchecked")
    static protected NamedList<String> castToNamedListOfString(Object o)
    {
        return (NamedList<String>) o;
    }

    /**
     * Casts an object to a Map of String, Object.
     *
     * The only reason this method exists is to isolate the SuppressWarnings decoration.
     *
     * @param o
     * @return
     */
    @SuppressWarnings("unchecked")
    static protected Map<String,Object> castToMapOfStringObject(Object o)
    {
        return (Map<String,Object>) o;
    }

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
                _header = castToNamedListOfString(res.getVal(i));
            } else if ("Browse".equals(n)) {
                _browse = castToMapOfStringObject(res.getVal(i));
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
