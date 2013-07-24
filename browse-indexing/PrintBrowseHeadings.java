//
// Author: Mark Triggs <mark@dishevelled.net>
//

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.lucene.store.*;
import org.apache.lucene.search.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;

import java.sql.*;

import org.vufind.util.BrowseEntry;

// Note that this version is coming from Solr!
import org.apache.commons.codec.binary.Base64;


public class PrintBrowseHeadings
{
    private Leech bibLeech;
    private Leech authLeech;
    private Leech nonprefAuthLeech;

    IndexSearcher bibSearcher;
    IndexSearcher authSearcher;

    private String luceneField;

    private String KEY_SEPARATOR = "\1";
    private String RECORD_SEPARATOR = "\r\n";

    private void loadHeadings (Leech leech,
                               PrintWriter out,
                               Predicate predicate)
        throws Exception
    {
        BrowseEntry h;
        while ((h = leech.next ()) != null) {
            byte[] sort_key = h.key;
            String heading = h.value;

            if (predicate != null &&
                !predicate.isSatisfiedBy (heading)) {
                continue;
            }

            if (sort_key != null) {
                out.print (new String (Base64.encodeBase64 (sort_key)) +
                           KEY_SEPARATOR +
                           heading +
                           RECORD_SEPARATOR);
            }
        }
    }


    private int bibCount (String heading) throws IOException
    {
        TotalHitCountCollector counter = new TotalHitCountCollector();

        bibSearcher.search (new ConstantScoreQuery(new TermQuery (new Term (luceneField, heading))),
                            counter);

        return counter.getTotalHits ();
    }


    private boolean isLinkedFromBibData (String heading)
        throws IOException
    {
        TopDocs hits = null;

        int max_headings = 20;
        while (true) {
            hits = authSearcher.search
                (new ConstantScoreQuery
                 (new TermQuery
                  (new Term
                   (System.getProperty ("field.insteadof", "insteadOf"),
                    heading))),
                 max_headings);

            if (hits.scoreDocs.length < max_headings) {
                // That's all of them.  All done.
                break;
            } else {
                // Hm.  That's a lot of headings.  Go back for more.
                max_headings *= 2;
            }
        }

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            Document doc = authSearcher.getIndexReader ().document (hits.scoreDocs[i].doc);

            String[] preferred = doc.getValues (System.getProperty ("field.preferred", "preferred"));
            if (preferred.length > 0) {
                String preferredHeading = preferred[0];

                if (bibCount (preferredHeading) > 0) {
                    return true;
                }
            } else {
                return false;
            }
        }

        return false;
    }


    private String getEnvironment (String var)
    {
        return (System.getenv (var) != null) ?
            System.getenv (var) : System.getProperty (var.toLowerCase ());
    }


    private Leech getBibLeech (String bibPath, String luceneField)
        throws Exception
    {
        String leechClass = "Leech";

        if (getEnvironment ("BIBLEECH") != null) {
            leechClass = getEnvironment ("BIBLEECH");
        }

        return (Leech) (Class.forName (leechClass)
                        .getConstructor (String.class, String.class)
                        .newInstance (bibPath, luceneField ));
    }


    public void create (String bibPath,
                        String luceneField,
                        String authPath,
                        String outFile)
        throws Exception
    {
        bibLeech = getBibLeech (bibPath, luceneField);
        this.luceneField = luceneField;

        IndexReader bibReader = DirectoryReader.open (FSDirectory.open (new File (bibPath)));
        bibSearcher = new IndexSearcher (bibReader);

        PrintWriter out = new PrintWriter (new FileWriter (outFile));

        if (authPath != null) {
            nonprefAuthLeech = new Leech (authPath,
                                          System.getProperty ("field.insteadof",
                                                              "insteadOf"));

            IndexReader authReader = DirectoryReader.open (FSDirectory.open (new File (authPath)));
            authSearcher = new IndexSearcher (authReader);

            loadHeadings (nonprefAuthLeech, out,
                          new Predicate () {
                              public boolean isSatisfiedBy (Object obj)
                              {
                                  String heading = (String) obj;

                                  try {
                                      return isLinkedFromBibData (heading);
                                  } catch (IOException e) {
                                      return true;
                                  }
                              }}
                );

            nonprefAuthLeech.dropOff ();
        }

        loadHeadings (bibLeech, out, null);

        bibLeech.dropOff ();

        out.close ();
    }


    public static void main (String args[])
        throws Exception
    {
        if (args.length != 3 && args.length != 4) {
            System.err.println
                ("Usage: PrintBrowseHeadings <bib index> <bib field> "
                 + "<auth index> <out file>");
            System.err.println ("\nor:\n");
            System.err.println
                ("Usage: PrintBrowseHeadings <bib index> <bib field>"
                 + " <out file>");

            System.exit (0);
        }

        PrintBrowseHeadings self = new PrintBrowseHeadings ();

        if (args.length == 4) {
            self.create (args[0], args[1], args[2], args[3]);
        } else {
            self.create (args[0], args[1], null, args[2]);
        }
    }
}
