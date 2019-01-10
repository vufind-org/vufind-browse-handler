//
// Author: Mark Triggs <mark@dishevelled.net>
//


package org.vufind.solr.handler;



import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.vufind.util.NormalizerFactory;

/*
 * TODO: Update JavaDoc to document browse query parameters.
 */
/**
 * Handles the browse request: looks up the heading, consults the biblio core number of hits
 * and the authority core for cross references.
 *
 * By default the name of the authority core is <code>authority</code>. This can be overridden
 * by setting the parameter <core>authCoreName</core> in the handler configuration in
 * <code>solrconfig.xml</code>.
 *
 * @author Mark Triggs
 * @author Tod Olson
 *
 */
public class BrowseRequestHandler extends RequestHandlerBase
{
    public static final String DFLT_AUTH_CORE_NAME = "authority";
    protected String authCoreName = null;

    private Map<String,BrowseSource> sources = new HashMap<> ();
    private SolrParams solrParams;


    /**
     *  RequestHandlerBase implements SolrRequestHandler. As of Solr 4.2.1,
     *  {@link SolrRequestHandler#init(NamedList args)} is not defined with a type.
     *  So there's a warning.
     */
    public void init(@SuppressWarnings("rawtypes") NamedList args)
    {
        super.init(args);

        solrParams = SolrParams.toSolrParams(args);

        authCoreName = solrParams.get("authCoreName", DFLT_AUTH_CORE_NAME);

        sources = new ConcurrentHashMap<> ();

        for (String source : Arrays.asList(solrParams.get
                                           ("sources").split(","))) {
            @SuppressWarnings("unchecked")
            NamedList<String> entry = (NamedList<String>)args.get(source);

            // TODO: what if maxBibListSize is not set?
            int maxBibListSize = 0;
            try {
                maxBibListSize = Integer.parseInt(entry.get("maxBibListSize"));
            } catch (NumberFormatException e) {
                // badly formatted param, leave as default -1
            }
            sources.put(source,
                        new BrowseSource(entry.get("DBpath"),
                                         entry.get("field"),
                                         entry.get("dropChars"),
                                         entry.get("normalizer"),
                                         // defaults to false if not set or malformed
                                         Boolean.parseBoolean(entry.get("retrieveBibId")),
                                         maxBibListSize));
        }
    }


    private int asInt(String s)
    {
        int value;
        try {
            return new Integer(s).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /*
     *  TODO: Research question:
     *  Should we convert result from HashMap to Solr util classes
     *  org.apache.solr.common.util.NamedList or
     *  org.apache.solr.common.util.SimpleOrderedMap?
     *  Same question for BrowseList and other returned object.
     *
     *  Is it worth porting to the Solr classes used for results?
     *  The javadoc for NamedList says it gives better access by index while
     *  preserving the order of elements, not so for HashMap.
     */


    @Override
    public void handleRequestBody(org.apache.solr.request.SolrQueryRequest req,
                                  org.apache.solr.response.SolrQueryResponse rsp)
    throws Exception
    {
        SolrParams p = req.getParams();

        String sourceName = p.get("source");
        String from = p.get("from");
        // TODO: change parameter name to "fields" for VF 6.0
        String fields = p.get("extras");

        int rowid = 1;
        if (p.get("rowid") != null) {
            rowid = asInt(p.get("rowid"));
        }

        int rows = asInt(p.get("rows"));

        int offset = (p.get("offset") != null) ? asInt(p.get("offset")) : 0;

        /*
         * TODO: invalid row parameter should return a 400 error
         */
        if (rows < 0) {
            throw new Exception("Invalid value for parameter: rows");
        }

        /*
         * TODO: invalid or missing source parameter should return a 400 error
         */
        if (sourceName == null || !sources.containsKey(sourceName)) {
            throw new Exception("Need a (valid) source parameter.");
        }


        BrowseSource source = sources.get(sourceName);

        SolrCore core = req.getCore();
        CoreDescriptor cd = core.getCoreDescriptor();
        CoreContainer cc = core.getCoreContainer();
        SolrCore authCore = cc.getCore(authCoreName);
        //Must decrement RefCounted when finished!
        RefCounted<SolrIndexSearcher> authSearcherRef = authCore.getSearcher();

        HeadingsDB headingsDB = null;

        try {
            headingsDB = source.getHeadingsDB();
            SolrIndexSearcher authSearcher = authSearcherRef.get();

            Browse browse = new Browse(headingsDB,
                                       new BibDB(req.getSearcher(), source.field),
                                       new AuthDB
                                       (authSearcher,
                                        solrParams.get("preferredHeadingField"),
                                        solrParams.get("useInsteadHeadingField"),
                                        solrParams.get("seeAlsoHeadingField"),
                                        solrParams.get("scopeNoteField")),
                                       source.retrieveBibId,
                                       source.maxBibListSize);
            Log.info("new browse source with HeadingsDB (" + source.DBpath + ", " + source.normalizer + ")");

            if (from != null) {
                rowid = (browse.getId(from));
            }


            Log.info("Browsing from: " + rowid);

            BrowseList list = browse.getList(rowid, offset, rows, fields);

            Map<String,Object> result = new HashMap<>();

            result.put("totalCount", list.totalCount);
            result.put("items", list);
            result.put("startRow", rowid);
            result.put("offset", offset);

            new MatchTypeResponse(from, list, rowid, rows, offset, NormalizerFactory.getNormalizer(source.normalizer)).addTo(result);

            rsp.add("Browse", result);
        } finally {
            //Must decrement RefCounted when finished!
            authSearcherRef.decref();

            if (headingsDB != null) {
                source.returnHeadingsDB(headingsDB);
            }
        }
    }


    //////////////////////// SolrInfoMBeans methods //////////////////////

    public String getVersion()
    {
        return "$Revision: 0.1 $";
    }

    public String getDescription()
    {
        return "NLA browse handler";
    }

    public String getSourceId()
    {
        return "";
    }

    public String getSource()
    {
        return "";
    }

    public URL[] getDocs()
    {
        return null;
    }
}
