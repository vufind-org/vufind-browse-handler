//
// Author: Mark Triggs <mark@dishevelled.net>
//


package org.vufind.solr.handler;


import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.solr.handler.*;
import org.apache.solr.request.*;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.sql.*;

import org.vufind.util.*;
import org.apache.lucene.search.*;
import org.apache.lucene.document.*;
import java.util.logging.Logger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.vufind.util.Normaliser;
import org.vufind.util.BrowseEntry;

class Log
{
    private static Logger log ()
    {
        // Caller's class
        return Logger.getLogger
            (new Throwable ().getStackTrace ()[2].getClassName ());
    }


    public static void info (String s) { log ().info (s); }
}



class HeadingSlice
{
    public List<String> headings = new LinkedList<String> ();
    public int total;
}



class HeadingsDB
{
    Connection db;
    String path;
    long dbVersion;
    int totalCount;
    Normaliser normaliser;

    ReentrantReadWriteLock dbLock = new ReentrantReadWriteLock ();

    public HeadingsDB (String path) throws Exception
    {
        this.path = path;
        normaliser = Normaliser.getInstance ();
    }


    private void openDB () throws Exception
    {
        if (!new File (path).exists()) {
            throw new Exception ("I couldn't find a browse index at: " + path +
                                 ".\nMaybe you need to create your browse indexes?");
        }

        Class.forName ("org.sqlite.JDBC");

        db = DriverManager.getConnection ("jdbc:sqlite:" + path);
        db.setAutoCommit (false);
        dbVersion = currentVersion ();

        PreparedStatement countStmnt = db.prepareStatement (
            "select count(1) as count from headings");

        ResultSet rs = countStmnt.executeQuery ();
        rs.next ();

        totalCount = rs.getInt ("count");

        rs.close ();
        countStmnt.close ();
    }


    private long currentVersion ()
    {
        return new File (path).lastModified ();
    }


    synchronized public void reopenIfUpdated () throws Exception
    {
        dbLock.readLock ().lock ();

        File flag = new File (path + "-ready");
        File updated = new File (path + "-updated");
        if (db == null || (flag.exists () && updated.exists ())) {
            Log.info ("Index update event detected!");
            try {
                dbLock.readLock ().unlock ();
                dbLock.writeLock ().lock ();

                if (flag.exists () && updated.exists ()) {
                    Log.info ("Installing new index version...");
                    if (db != null) {
                        db.close ();
                    }

                    File pathFile = new File (path);
                    pathFile.delete ();
                    updated.renameTo (pathFile);
                    flag.delete ();

                    Log.info ("Reopening HeadingsDB");
                    openDB ();
                } else if (db == null) {
                    openDB ();
                }
            } finally {
                dbLock.readLock ().lock ();
                dbLock.writeLock ().unlock ();
            }
        }
    }

    public void queryFinished ()
    {
        dbLock.readLock ().unlock ();
    }


    public int getHeadingStart (String from) throws Exception
    {
        PreparedStatement rowStmnt = db.prepareStatement (
            "select rowid from headings " +
            "where key >= ? " +
            "order by key " +
            "limit 1");

        rowStmnt.setBytes (1, normaliser.normalise (from));

        ResultSet rs = rowStmnt.executeQuery ();

        if (rs.next ()) {
            return rs.getInt ("rowid");
        } else {
            return totalCount + 1;   // past the end
        }
    }


    public HeadingSlice getHeadings (int rowid,
                                     int rows)
        throws Exception
    {
        HeadingSlice result = new HeadingSlice ();

        PreparedStatement rowStmnt = db.prepareStatement (
            String.format ("select * from headings " +
                           "where rowid >= ? " +
                           "order by rowid " +
                           "limit %d ",
                           rows)
            );

        rowStmnt.setInt (1, rowid);

        ResultSet rs = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                rs = rowStmnt.executeQuery ();
                break;
            } catch (SQLException e) {
                Log.info ("Retry number " + attempt + "...");
                Thread.sleep (50);
            }
        }

        if (rs == null) {
            return result;
        }

        while (rs.next ()) {
            result.headings.add (rs.getString ("heading"));
        }

        rs.close ();
        rowStmnt.close ();

        result.total = (totalCount - rowid) + 1;

        return result;
    }
}



class LuceneDB
{
    static Map<String,LuceneDB> dbs = new HashMap<String,LuceneDB> ();

    IndexSearcher searcher;
    String dbpath;
    long currentVersion = -1;


    public synchronized static LuceneDB getOrCreate (String path)
        throws Exception
    {
        if (!dbs.containsKey (path)) {
            LuceneDB db = new LuceneDB (path);
            dbs.put (path, db);
        }

        return dbs.get (path);
    }


    public synchronized static void reopenAllIfUpdated ()
        throws Exception
    {
        for (LuceneDB db : dbs.values ()) {
            db.reopenIfUpdated ();
        }
    }


    public LuceneDB (String path) throws Exception
    {
        this.dbpath = path;
    }


    private void openSearcher () throws Exception
    {
        if (searcher != null) {
            searcher.close ();
        }

        IndexReader dbReader = IndexReader.open(FSDirectory.open (new File (dbpath)));
        searcher = new IndexSearcher (dbReader);
        currentVersion = indexVersion ();
    }


    public TopDocs search (Query q, int n) throws Exception
    {
        return searcher.search (q, n);
    }


    private long indexVersion ()
    {
        return new File (dbpath + "/segments.gen").lastModified ();
    }


    private boolean isDBUpdated ()
    {
        return (currentVersion != indexVersion ());
    }


    public Document getDocument (int docid) throws Exception
    {
        return searcher.getIndexReader ().document (docid);
    }


    public synchronized void reopenIfUpdated () throws Exception
    {
        if (isDBUpdated ()) {
            openSearcher ();
            Log.info ("Reopened " + searcher + " (" + dbpath + ")");
        }
    }
}



class AuthDB
{
    static int MAX_PREFERRED_HEADINGS = 1000;

    private LuceneDB db;
    private String preferredHeadingField;
    private String useInsteadHeadingField;
    private String seeAlsoHeadingField;
    private String scopeNoteField;

    public AuthDB (String path,
                   String preferredField,
                   String useInsteadField,
                   String seeAlsoField,
                   String noteField)
        throws Exception
    {
        db = LuceneDB.getOrCreate (path);
        preferredHeadingField = preferredField;
        useInsteadHeadingField = useInsteadField;
        seeAlsoHeadingField = seeAlsoField;
        scopeNoteField = noteField;
    }


    private List<String> docValues (Document doc, String field)
    {
        String values[] = doc.getValues (field);

        if (values == null) {
            values = new String[] {};
        }

        return Arrays.asList (values);
    }


    public void reopenIfUpdated () throws Exception
    {
        db.reopenIfUpdated ();
    }


    public Document getAuthorityRecord (String heading)
        throws Exception
    {
        TopDocs results = (db.search (new TermQuery (new Term (preferredHeadingField,
                                                               heading)),
                                      1));

        if (results.totalHits > 0) {
            return db.getDocument (results.scoreDocs[0].doc);
        } else {
            return null;
        }
    }


    public List<Document> getPreferredHeadings (String heading)
        throws Exception
    {
        TopDocs results = (db.search (new TermQuery (new Term (useInsteadHeadingField,
                                                               heading)),
                                      MAX_PREFERRED_HEADINGS));

        List<Document> result = new Vector<Document> ();

        for (int i = 0; i < results.totalHits; i++) {
            result.add (db.getDocument (results.scoreDocs[i].doc));
        }

        return result;
    }


    public Map<String, List<String>> getFields (String heading)
        throws Exception
    {
        Document authInfo = getAuthorityRecord (heading);

        Map<String, List<String>> itemValues =
            new HashMap<String,List<String>> ();

        itemValues.put ("seeAlso", new ArrayList<String>());
        itemValues.put ("useInstead", new ArrayList<String>());
        itemValues.put ("note", new ArrayList<String>());

        if (authInfo != null) {
            for (String value : docValues (authInfo, seeAlsoHeadingField)) {
                itemValues.get ("seeAlso").add (value);
            }

            for (String value : docValues (authInfo, scopeNoteField)) {
                itemValues.get ("note").add (value);
            }
        } else {
            List<Document> preferredHeadings =
                getPreferredHeadings (heading);

            for (Document doc : preferredHeadings) {
                for (String value : docValues (doc, preferredHeadingField)) {
                    itemValues.get ("useInstead").add (value);
                }
            }
        }

        return itemValues;
    }
}



class BibDB
{
    private IndexSearcher db;
    private String field;

    public BibDB (IndexSearcher searcher, String field) throws Exception
    {
        db = searcher;
        this.field = field;
    }


    public int recordCount (String heading)
        throws Exception
    {
        TermQuery q = new TermQuery (new Term (field, heading));

        Log.info ("Searching '" + field + "' for '" + "'" + heading + "'");

        TotalHitCountCollector counter = new TotalHitCountCollector();
        db.search (q, counter);

        Log.info ("Hits: " + counter.getTotalHits ());

        return counter.getTotalHits ();
    }


    public List<String> matchingIDs (String heading)
        throws Exception
    {
        TermQuery q = new TermQuery (new Term (field, heading));

        Log.info (System.currentTimeMillis () + " Searching '" + field + "' for '" + heading + "'");

        final List<String> ids = new ArrayList<String> ();

        db.search (q, new Collector () {
                private int docBase;

                public void setScorer (Scorer scorer) {
                }

                public boolean acceptsDocsOutOfOrder () {
                    return true;
                }

                public void collect (int docnum) {
                    int docid = docnum + docBase;
                    try {
                        Document doc = db.getIndexReader ().document (docid);

                        String[] vals = doc.getValues ("id");
                        ids.add (vals[0]);
                    } catch (org.apache.lucene.index.CorruptIndexException e) {
                        Log.info ("CORRUPT INDEX EXCEPTION.  EEK! - " + e);
                    } catch (Exception e) {
                        Log.info ("Exception thrown: " + e);
                    }

                }

                public void setNextReader (IndexReader reader, int docBase) {
                    this.docBase = docBase;
                }
            });

        return ids;
    }
}



class BrowseList
{
    public int totalCount;
    public List<BrowseItem> items = new LinkedList<BrowseItem> ();


    public List<Map<String, Object>> asMap ()
    {
        List<Map<String, Object>> result = new LinkedList<Map<String, Object>> ();

        for (BrowseItem item : items) {
            result.add (item.asMap ());
        }

        return result;
    }
}



class BrowseItem
{
    public List<String> seeAlso = new LinkedList<String> ();
    public List<String> useInstead = new LinkedList<String> ();
    public String note = "";
    public String heading;
    public List<String> ids;
    int count;


    public BrowseItem (String heading)
    {
        this.heading = heading;
    }


    public Map<String, Object> asMap ()
    {
        Map<String, Object> result = new HashMap<String, Object> ();

        result.put ("heading", heading);
        result.put ("seeAlso", seeAlso);
        result.put ("useInstead", useInstead);
        result.put ("note", note);
        result.put ("count", new Integer (count));
        result.put ("ids", ids);

        return result;
    }
}




class Browse
{
    private HeadingsDB headingsDB;
    private AuthDB authDB;
    private BibDB bibDB;


    public Browse (HeadingsDB headings, AuthDB auth)
    {
        headingsDB = headings;
        authDB = auth;
    }


    public void setBibDB (BibDB b)
    {
        this.bibDB = b;
    }


    public synchronized void reopenDatabasesIfUpdated () throws Exception
    {
        headingsDB.reopenIfUpdated ();
        authDB.reopenIfUpdated ();
    }


    public void queryFinished ()
    {
        headingsDB.queryFinished ();
    }


    private void populateItem (BrowseItem item) throws Exception
    {
        List<String> ids = bibDB.matchingIDs (item.heading);
        item.ids = ids;
        item.count = ids.size ();

        Map<String, List<String>> fields = authDB.getFields (item.heading);

        for (String value : fields.get ("seeAlso")) {
            if (bibDB.recordCount (value) > 0) {
                item.seeAlso.add (value);
            }
        }

        for (String value : fields.get ("useInstead")) {
            if (bibDB.recordCount (value) > 0) {
                item.useInstead.add (value);
            }
        }

        for (String value : fields.get ("note")) {
            item.note = value;
        }
    }


    public int getId (String from) throws Exception
    {
        return headingsDB.getHeadingStart (from);
    }


    public BrowseList getList (int rowid, int offset, int rows)
        throws Exception
    {
        BrowseList result = new BrowseList ();

        HeadingSlice h = headingsDB.getHeadings (Math.max (0, rowid + offset),
                                                 rows);

        result.totalCount = h.total;

        for (String heading : h.headings) {
            BrowseItem item = new BrowseItem (heading);

            populateItem (item);

            result.items.add (item);
        }

        return result;
    }
}



class BrowseSource
{
    public String DBpath;
    public String field;
    public String dropChars;

    public Browse browse;


    public BrowseSource (String DBpath,
                         String field,
                         String dropChars)
    {
        this.DBpath = DBpath;
        this.field = field;
        this.dropChars = dropChars;
    }
}



public class BrowseRequestHandler extends RequestHandlerBase
{
    private String authPath = null;
    private String bibPath = null;

    private Map<String,BrowseSource> sources = new HashMap<String,BrowseSource> ();

    private SolrParams solrParams;


    private String asAbsFile (String s)
    {
        File f = new File (s);

        if (!f.isAbsolute ()) {
            return (new File (new File (System.getenv ("BROWSE_HOME")),
                             f.getPath ()).getPath ());
        } else {
            return f.getPath ();
        }
    }


    public void init (NamedList args)
    {
        super.init (args);

        solrParams = SolrParams.toSolrParams (args);

        authPath = asAbsFile (solrParams.get ("authIndexPath"));
        bibPath = asAbsFile (solrParams.get ("bibIndexPath"));

        sources = new HashMap<String, BrowseSource> ();

        for (String source : Arrays.asList (solrParams.get
                                            ("sources").split (","))) {
            @SuppressWarnings("unchecked")
            NamedList<String> entry = (NamedList<String>)args.get (source);

            sources.put (source,
                         new BrowseSource (entry.get ("DBpath"),
                                           entry.get ("field"),
                                           entry.get ("dropChars")));
        }
    }


    private int asInt (String s)
    {
        int value;
        try {
            return new Integer (s).intValue ();
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    @Override
    public void handleRequestBody (org.apache.solr.request.SolrQueryRequest req,
                                   org.apache.solr.response.SolrQueryResponse rsp)
        throws Exception
    {
        SolrParams p = req.getParams ();

        if (p.get ("reopen") != null) {
            LuceneDB.reopenAllIfUpdated ();
            return;
        }


        String sourceName = p.get ("source");
        String from = p.get ("from");

        int rowid = 1;
        if (p.get ("rowid") != null) {
            rowid = asInt (p.get ("rowid"));
        }

        int rows = asInt (p.get ("rows"));

        int offset = (p.get ("offset") != null) ? asInt (p.get ("offset")) : 0;

        if (rows < 0) {
            throw new Exception ("Invalid value for parameter: rows");
        }

        if (sourceName == null || !sources.containsKey (sourceName)) {
            throw new Exception ("Need a (valid) source parameter.");
        }


        BrowseSource source = sources.get (sourceName);

        synchronized (this) {
            if (source.browse == null) {
                source.browse = (new Browse
                                 (new HeadingsDB (source.DBpath),
                                  new AuthDB
                                  (authPath,
                                   solrParams.get ("preferredHeadingField"),
                                   solrParams.get ("useInsteadHeadingField"),
                                   solrParams.get ("seeAlsoHeadingField"),
                                   solrParams.get ("scopeNoteField"))));
            }

            source.browse.setBibDB (new BibDB (req.getSearcher (),
                                               source.field));
        }

        try {
            source.browse.reopenDatabasesIfUpdated ();

            if (from != null) {
                rowid = (source.browse.getId (from));
            }


            Log.info ("Browsing from: " + rowid);

            BrowseList list = source.browse.getList (rowid, offset, rows);

            Map<String,Object> result = new HashMap<String, Object> ();

            result.put ("totalCount", list.totalCount);
            result.put ("items", list.asMap ());
            result.put ("startRow", rowid);
            result.put ("offset", offset);

            rsp.add ("Browse", result);
        } finally {
            source.browse.queryFinished ();
        }
    }


    //////////////////////// SolrInfoMBeans methods //////////////////////

    public String getVersion () {
        return "$Revision: 0.1 $";
    }

    public String getDescription () {
        return "NLA browse handler";
    }

    public String getSourceId () {
        return "";
    }

    public String getSource () {
        return "";
    }

    public URL[] getDocs () {
        return null;
    }
}
