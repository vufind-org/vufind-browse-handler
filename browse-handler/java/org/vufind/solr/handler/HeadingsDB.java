package org.vufind.solr.handler;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.vufind.util.Normalizer;
import org.vufind.util.NormalizerFactory;

/**
 * Class to interact with a browse index and return a slice of the index.
 * <p>
 * Usually created by {@link BrowseSource}.
 *
 */
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