package org.vufind.solr.handler.client.solrj;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.SolrRequestType;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

/**
 * Client class for sending queries to {@link BrowseRequestHandler}.
 *
 * This is a SolrJ class for ease of querying the BrowseRequestHandler.
 * Currently used only for testing, but possibly of more general use.
 *
 * @author Tod Olson <tod@uchicago.edu>
 *
 */
@SuppressWarnings("serial")
public class BrowseRequest extends SolrRequest<BrowseResponse>
{

    private SolrParams query = null;

    /**
     * Assume browse handler is configured at "/browse".
     */
    public static String path = "/browse";

    public BrowseRequest()
    {
        super(METHOD.GET, path, SolrRequestType.QUERY);
    }

    public BrowseRequest(SolrParams q)
    {
        super(METHOD.GET, path, SolrRequestType.QUERY);
        query = q;
    }


    public BrowseRequest(METHOD m, String path)
    {
        super(m, path, SolrRequestType.QUERY);
        // TODO Auto-generated constructor stub
    }

    @Override
    public SolrRequestType getRequestType()
    {
        return SolrRequestType.QUERY;
    }

    @Override
    public SolrParams getParams()
    {
        return query;
    }

    @Override
    protected BrowseResponse createResponse(NamedList<Object> res)
    {
        return new BrowseResponse(res);
    }

}
