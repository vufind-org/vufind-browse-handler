//
// Author: Mark Triggs <mark@dishevelled.net>
//


package org.vufind.solr.handler;



import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.vufind.util.Normalizer;
import org.vufind.util.NormalizerFactory;

class HeadingSlice
{
    public List<String> sort_keys = new ArrayList<> ();
    public List<String> headings = new ArrayList<> ();
    public int total;
}



class HeadingsDB
{
    Connection db;
    String path;
    long dbVersion;
    /** The total number of headings in this DB. */
    int totalCount;
    Normalizer normalizer;

    public HeadingsDB(String path)
    {
        try {
            this.path = path;
            normalizer = NormalizerFactory.getNormalizer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HeadingsDB(String path, String normalizerClassName)
    {
        Log.info("constructor: HeadingsDB (" + path + ", " + normalizerClassName + ")");
        try {
            this.path = path;
            normalizer = NormalizerFactory.getNormalizer(normalizerClassName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void openDB() throws Exception
    {
        if (!new File(path).exists()) {
            throw new Exception("I couldn't find a browse index at: " + path +
                                ".\nMaybe you need to create your browse indexes?");
        }

        Class.forName("org.sqlite.JDBC");

        db = DriverManager.getConnection("jdbc:sqlite:" + path);
        db.setAutoCommit(false);
        dbVersion = currentVersion();

        PreparedStatement countStmnt = db.prepareStatement(
                                           "select count(1) as count from headings");

        ResultSet rs = countStmnt.executeQuery();
        rs.next();

        totalCount = rs.getInt("count");

        rs.close();
        countStmnt.close();
    }


    private long currentVersion()
    {
        return new File(path).lastModified();
    }


    public void reopenIfUpdated()
    {
        File flag = new File(path + "-ready");
        File updated = new File(path + "-updated");
        if (db == null || (flag.exists() && updated.exists())) {
            Log.info("Index update event detected!");
            try {
                if (flag.exists() && updated.exists()) {
                    Log.info("Installing new index version...");
                    if (db != null) {
                        db.close();
                    }

                    File pathFile = new File(path);
                    pathFile.delete();
                    updated.renameTo(pathFile);
                    flag.delete();

                    Log.info("Reopening HeadingsDB");
                    openDB();
                } else if (db == null) {
                    openDB();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized int getHeadingStart(String from) throws Exception
    {
        PreparedStatement rowStmnt = db.prepareStatement(
                                         "select rowid from headings " +
                                         "where key >= ? " +
                                         "order by key " +
                                         "limit 1");

        rowStmnt.setBytes(1, normalizer.normalize(from));

        ResultSet rs = rowStmnt.executeQuery();

        if (rs.next()) {
            return rs.getInt("rowid");
        } else {
            return totalCount + 1;   // past the end
        }
    }


    public synchronized HeadingSlice getHeadings(int rowid,
            int rows)
    throws Exception
    {
        HeadingSlice result = new HeadingSlice();

        PreparedStatement rowStmnt = db.prepareStatement(
                                         String.format("select * from headings " +
                                                 "where rowid >= ? " +
                                                 "order by rowid " +
                                                 "limit %d ",
                                                 rows)
                                     );

        rowStmnt.setInt(1, rowid);

        ResultSet rs = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                rs = rowStmnt.executeQuery();
                break;
            } catch (SQLException e) {
                Log.info("Retry number " + attempt + "...");
                Thread.sleep(50);
            }
        }

        if (rs == null) {
            return result;
        }

        while (rs.next()) {
            result.sort_keys.add(rs.getString("key_text"));
            result.headings.add(rs.getString("heading"));
        }

        rs.close();
        rowStmnt.close();

        result.total = Math.max(0, (totalCount - rowid) + 1);

        return result;
    }
}


/**
 * Class that performs the alphabetical browse of an index and produces a {@code BrowseList} object.
 *
 */
class Browse
{
    private HeadingsDB headingsDB;
    private AuthDB authDB;
    private BibDB bibDB;
    private int maxBibListSize;

    public Browse(HeadingsDB headings, BibDB bibdb, AuthDB auth, boolean retrieveBibId, int maxBibListSize)
    {
        this.headingsDB = headings;
        this.authDB = auth;
        this.bibDB = bibdb;
        this.maxBibListSize = maxBibListSize;
    }

    private void populateItem(BrowseItem item, String fields) throws Exception
    {
        Map<String, List<Collection<String>>> bibinfo =
            bibDB.matchingExtras(item.getHeading(), fields, maxBibListSize);
        item.setExtras(bibinfo);
        item.setCount(bibDB.recordCount(item.getHeading()));

        Map<String, List<String>> authFields = authDB.getFields(item.getHeading());

        List<String> seeAlsoList = new ArrayList<String>();
        for (String value : authFields.get("seeAlso")) {
            if (bibDB.recordCount(value) > 0) {
                seeAlsoList.add(value);
            }
        }
        item.setSeeAlso(seeAlsoList);

        List<String> useInsteadList = new ArrayList<String>();
        for (String value : authFields.get("useInstead")) {
            if (bibDB.recordCount(value) > 0) {
                useInsteadList.add(value);
            }
        }
        item.setUseInstead(useInsteadList);

        for (String value : authFields.get("note")) {
            item.setNote(value);
        }
    }


    public int getId(String from) throws Exception
    {
        return headingsDB.getHeadingStart(from);
    }


    public BrowseList getList(int rowid, int offset, int rows, String extras)
    throws Exception
    {
        BrowseList result = new BrowseList();

        HeadingSlice h = headingsDB.getHeadings(Math.max(1, rowid + offset),
                                                rows);

        result.totalCount = h.total;

        for (int i = 0; i < h.headings.size(); i++) {
            String heading = h.headings.get(i);
            String sort_key = h.sort_keys.get(i);

            BrowseItem item = new BrowseItem(sort_key, heading);

            populateItem(item, extras);

            result.add(item);
        }

        return result;
    }
}


class BrowseSource
{
    public String DBpath;
    public String field;
    public String dropChars;
    public String normalizer;
    public boolean retrieveBibId;
    public int maxBibListSize;

    private HeadingsDB headingsDB = null;
    private long loanCount = 0;


    public BrowseSource(String DBpath,
                        String field,
                        String dropChars,
                        String normalizer,
                        boolean retrieveBibId,
                        int maxBibListSize)
    {
        this.DBpath = DBpath;
        this.field = field;
        this.dropChars = dropChars;
        this.normalizer = normalizer;
        this.retrieveBibId = retrieveBibId;
        this.maxBibListSize = maxBibListSize;
    }

    // Get a HeadingsDB instance.  Caller is expected to call `queryFinished` on
    // this when done with the instance.
    public synchronized HeadingsDB getHeadingsDB()
    {
        if (headingsDB == null) {
            headingsDB = new HeadingsDB(this.DBpath, this.normalizer);
        }

        // If no queries are running, it's a safe point to reopen the browse index.
        if (loanCount <= 0) {
            headingsDB.reopenIfUpdated();
            loanCount = 0;
        }

        loanCount += 1;

        return headingsDB;
    }

    public synchronized void returnHeadingsDB(HeadingsDB headingsDB)
    {
        loanCount -= 1;
    }
}


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
