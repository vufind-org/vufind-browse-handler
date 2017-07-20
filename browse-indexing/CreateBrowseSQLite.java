//
// Author: Mark Triggs <mark@dishevelled.net>
//

import java.io.*;

import java.sql.*;

// Note that this version is coming from Solr!
import org.apache.commons.codec.binary.Base64;


public class CreateBrowseSQLite
{
    private Connection outputDB;

    private String KEY_SEPARATOR = "\1";


    /*
     * Like BufferedReader#readLine(), but only returns lines ended by a \r\n.
     */
    private String readCRLFLine(BufferedReader br) throws IOException
    {
        StringBuilder sb = new StringBuilder();

        while (true) {
            int ch = br.read();

            if (ch >= 0) {
                if (ch == '\r') {
                    // This might either be a carriage return embedded in record
                    // data (which we want to preserve) or the first part of the
                    // \r\n end of line marker.

                    ch = br.read();

                    if (ch == '\n') {
                        // An end of line.  We're done.
                        return sb.toString();
                    }

                    // Must have been an embedded carriage return.  Keep it.
                    sb.append('\r');
                }

                sb.append((char) ch);
            } else {
                // EOF.  Show's over.
                return null;
            }
        }
    }


    private void loadHeadings(BufferedReader br)
    throws Exception
    {
        int count = 0;

        outputDB.setAutoCommit(false);

        PreparedStatement prep = outputDB.prepareStatement(
                                     "insert or ignore into all_headings (key, key_text, heading) values (?, ?, ?)");

        String line;
        while ((line = readCRLFLine(br)) != null) {
            String[] fields = line.split(KEY_SEPARATOR);

            if (fields.length == 3) {
                // If we found the separator character, we have a key/value pair of
                // Base64-encoded strings to decode and push into the batch:
                prep.setBytes(1, Base64.decodeBase64(fields[0].getBytes()));
                prep.setBytes(2, Base64.decodeBase64(fields[1].getBytes()));
                prep.setBytes(3, Base64.decodeBase64(fields[2].getBytes()));

                prep.addBatch();
            }

            if ((count % 500000) == 0) {
                prep.executeBatch();
                prep.clearBatch();
            }

            count++;
        }

        prep.executeBatch();
        prep.close();

        outputDB.commit();
        outputDB.setAutoCommit(true);
    }


    private void setupDatabase()
    throws Exception
    {
        Statement stat = outputDB.createStatement();

        stat.executeUpdate("drop table if exists all_headings;");
        stat.executeUpdate("create table all_headings (key, key_text, heading);");
        stat.executeUpdate("PRAGMA synchronous = OFF;");
        stat.execute("PRAGMA journal_mode = OFF;");

        stat.close();
    }


    private void buildOrderedTables()
    throws Exception
    {
        Statement stat = outputDB.createStatement();

        stat.executeUpdate("drop table if exists headings;");
        stat.executeUpdate("create table headings " +
                           "as select * from all_headings order by key;");

        stat.executeUpdate("create index keyindex on headings (key);");

        stat.close();
    }


    public void create(String headingsFile, String outputPath)
    throws Exception
    {
        Class.forName("org.sqlite.JDBC");
        outputDB = DriverManager.getConnection("jdbc:sqlite:" + outputPath);

        setupDatabase();

        BufferedReader br = new BufferedReader
        (new FileReader(headingsFile));

        loadHeadings(br);

        br.close();

        buildOrderedTables();
    }


    public static void main(String args[])
    throws Exception
    {
        if (args.length != 2) {
            System.err.println
            ("Usage: CreateBrowseSQLite <headings file> <db file>");
            System.exit(0);
        }

        CreateBrowseSQLite self = new CreateBrowseSQLite();

        self.create(args[0], args[1]);
    }
}
