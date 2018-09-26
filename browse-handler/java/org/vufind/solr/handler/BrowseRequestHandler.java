//
// Author: Mark Triggs <mark@dishevelled.net>
//


package org.vufind.solr.handler;



import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.solr.handler.*;
import org.apache.solr.request.*;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;

import java.io.File;
import java.io.UnsupportedEncodingException;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.vufind.util.*;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.document.Document;

import java.util.logging.Logger;

import org.vufind.util.Normalizer;
import org.vufind.util.NormalizerFactory;
import org.vufind.util.BrowseEntry;

class Log
{
    private static Logger log()
    {
        // Caller's class
        return Logger.getLogger
               (new Throwable().getStackTrace()[2].getClassName());
    }


    public static String formatBytes(byte[] bytes)
    {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append("0x");
            result.append(Integer.toHexString(bytes[i]));
        }

        return result.toString();
    }


    public static String formatBytes(String s)
    {
        try {
            return formatBytes(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void info(String s)
    {
        log().info(s);
    }

    public static void info(String fmt, String s)
    {
        log().info(String.format(fmt, s));
    }
}



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
 *
 * Interface to the Solr Authority DB
 *
 */
class AuthDB
{
    static int MAX_PREFERRED_HEADINGS = 1000;

    private SolrIndexSearcher searcher;
    private String preferredHeadingField;
    private String useInsteadHeadingField;
    private String seeAlsoHeadingField;
    private String scopeNoteField;

    public AuthDB(SolrIndexSearcher authSearcher,
                  String preferredField,
                  String useInsteadField,
                  String seeAlsoField,
                  String noteField)
    throws Exception
    {
        searcher = authSearcher;
        preferredHeadingField = preferredField;
        useInsteadHeadingField = useInsteadField;
        seeAlsoHeadingField = seeAlsoField;
        scopeNoteField = noteField;
    }


    private List<String> docValues(Document doc, String field)
    {
        String values[] = doc.getValues(field);

        if (values == null) {
            values = new String[] {};
        }

        return Arrays.asList(values);
    }


    public Document getAuthorityRecord(String heading)
    throws Exception
    {
        TopDocs results = (searcher.search(new TermQuery(new Term(preferredHeadingField,
                                           heading)),
                                           1));

        if (results.totalHits > 0) {
            return searcher.getIndexReader().document(results.scoreDocs[0].doc);
        } else {
            return null;
        }
    }


    public List<Document> getPreferredHeadings(String heading)
    throws Exception
    {
        TopDocs results = (searcher.search(new TermQuery(new Term(useInsteadHeadingField,
                                           heading)),
                                           MAX_PREFERRED_HEADINGS));

        List<Document> result = new ArrayList<> ();

        for (int i = 0; i < results.totalHits; i++) {
            result.add(searcher.getIndexReader().document(results.scoreDocs[i].doc));
        }

        return result;
    }


    public Map<String, List<String>> getFields(String heading)
    throws Exception
    {
        Document authInfo = getAuthorityRecord(heading);

        Map<String, List<String>> itemValues = new HashMap<> ();

        itemValues.put("seeAlso", new ArrayList<String>());
        itemValues.put("useInstead", new ArrayList<String>());
        itemValues.put("note", new ArrayList<String>());

        if (authInfo != null) {
            for (String value : docValues(authInfo, seeAlsoHeadingField)) {
                itemValues.get("seeAlso").add(value);
            }

            for (String value : docValues(authInfo, scopeNoteField)) {
                itemValues.get("note").add(value);
            }
        } else {
            List<Document> preferredHeadings =
                getPreferredHeadings(heading);

            for (Document doc : preferredHeadings) {
                for (String value : docValues(doc, preferredHeadingField)) {
                    itemValues.get("useInstead").add(value);
                }
            }
        }

        return itemValues;
    }
}



/**
 *
 * Interface to the Solr biblio db
 *
 */
class BibDB
{
    private IndexSearcher db;
    private String field;

    public BibDB(IndexSearcher searcher, String field) throws Exception
    {
        db = searcher;
        this.field = field;
    }


    public int recordCount(String heading)
    throws Exception
    {
        TermQuery q = new TermQuery(new Term(field, heading));

        Log.info("Searching '" + field + "' for '" + "'" + heading + "'");

        TotalHitCountCollector counter = new TotalHitCountCollector();
        db.search(q, counter);

        Log.info("Hits: " + counter.getTotalHits());

        return counter.getTotalHits();
    }


    /**
     *
     * Function to retireve the doc ids when there is a building limit
     * This retrieves the doc ids for an individual heading
     *
     * Need to add a filter query to limit the results from Solr
     *
     * Includes functionality to retrieve additional info
     * like titles for call numbers, possibly ISBNs
     *
     * @param heading  string of the heading to use for finding matching docs
     * @param extras   colon-separated string of extra Solr fields to return
     *                 for use in the browse display
     * @return         return a map of Solr ids and extra bib info
     */
    public Map<String, List<Collection<String>>> matchingIDs(String heading, String extras)
    throws Exception
    {
        TermQuery q = new TermQuery(new Term(field, heading));

        // bibinfo values are List<Collection> because some extra fields
        // may be multi-valued.
        // Note: it may be time for bibinfo to become a class...
        final Map<String, List<Collection<String>>> bibinfo = new HashMap<> ();
        bibinfo.put("ids", new ArrayList<Collection<String>> ());
        final String[] bibExtras = extras.split(":");
        for (String bibField : bibExtras) {
            bibinfo.put(bibField, new ArrayList<Collection<String>> ());
        }

        db.search(q, new SimpleCollector() {
            private LeafReaderContext context;

            public void setScorer(Scorer scorer) {
            }

            public boolean acceptsDocsOutOfOrder() {
                return true;
            }

            public boolean needsScores() {
                return false;
            }

            public void doSetNextReader(LeafReaderContext context) {
                this.context = context;
            }


            public void collect(int docnum) {
                int docid = docnum + context.docBase;
                try {
                    Document doc = db.getIndexReader().document(docid);

                    String[] vals = doc.getValues("id");
                    Collection<String> id = new HashSet<> ();
                    id.add(vals[0]);
                    bibinfo.get("ids").add(id);
                    for (String bibField : bibExtras) {
                        vals = doc.getValues(bibField);
                        if (vals.length > 0) {
                            Collection<String> valSet = new LinkedHashSet<> ();
                            for (String val : vals) {
                                valSet.add(val);
                            }
                            bibinfo.get(bibField).add(valSet);
                        }
                    }
                } catch (org.apache.lucene.index.CorruptIndexException e) {
                    Log.info("CORRUPT INDEX EXCEPTION.  EEK! - " + e);
                } catch (Exception e) {
                    Log.info("Exception thrown: " + e);
                }

            }
        });

        return bibinfo;
    }
}






class Browse
{
    private HeadingsDB headingsDB;
    private AuthDB authDB;
    private BibDB bibDB;

    public Browse(HeadingsDB headings, BibDB bibdb, AuthDB auth)
    {
        headingsDB = headings;
        authDB = auth;
        bibDB = bibdb;
    }

    private void populateItem(BrowseItem item, String extras) throws Exception
    {
        Map<String, List<Collection<String>>> bibinfo = bibDB.matchingIDs(item.heading, extras);
        //item.ids = bibinfo.get ("ids");
        item.setIds(bibinfo.get("ids"));
        bibinfo.remove("ids");
        item.count = item.ids.size();

        item.extras = bibinfo;

        Map<String, List<String>> fields = authDB.getFields(item.heading);

        for (String value : fields.get("seeAlso")) {
            if (bibDB.recordCount(value) > 0) {
                item.seeAlso.add(value);
            }
        }

        for (String value : fields.get("useInstead")) {
            if (bibDB.recordCount(value) > 0) {
                item.useInstead.add(value);
            }
        }

        for (String value : fields.get("note")) {
            item.note = value;
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

            result.items.add(item);
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

    private HeadingsDB headingsDB = null;
    private long loanCount = 0;


    public BrowseSource(String DBpath,
                        String field,
                        String dropChars,
                        String normalizer)
    {
        this.DBpath = DBpath;
        this.field = field;
        this.dropChars = dropChars;
        this.normalizer = normalizer;
    }

    // Get a HeadingsDB instance.  Caller is expected to call `queryFinished` on
    // this when done with the instance.
    public synchronized HeadingsDB getHeadingsDB()
    {
        if (headingsDB == null) {
            headingsDB = new HeadingsDB(this.DBpath, this.normalizer);
        }

        // If no queries are running, it's a safepoint to reopen the browse index.
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

            sources.put(source,
                        new BrowseSource(entry.get("DBpath"),
                                         entry.get("field"),
                                         entry.get("dropChars"),
                                         entry.get("normalizer")));
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
        String extras = p.get("extras");

        // extras needs to be a non-null string
        if (extras == null) {
            extras = "";
        }

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
                                        solrParams.get("scopeNoteField")));


            if (from != null) {
                rowid = (browse.getId(from));
            }


            Log.info("Browsing from: " + rowid);

            BrowseList list = browse.getList(rowid, offset, rows, extras);

            Map<String,Object> result = new HashMap<>();

            result.put("totalCount", list.totalCount);
            result.put("items", list.asMap());
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
