package org.vufind.solr.indexing;

//
// Author: Mark Triggs <mark@teaspoon-consulting.com>
//
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;

// Note that this version is coming from Solr!
import org.apache.commons.codec.binary.Base64;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.FSDirectory;
import org.vufind.util.BrowseEntry;
import org.vufind.util.Utils;


public class PrintBrowseHeadings
{
    private SolrFieldIterator nonprefAuthFieldIterator;

    IndexSearcher bibSearcher;
    IndexSearcher authSearcher;

    private String luceneField;

    private String KEY_SEPARATOR = "\1";
    private String RECORD_SEPARATOR = "\r\n";

    /**
     * Load headings from the index into a file.
     *
     * @param fieldIterator      SolrFieldIterator source for headings
     * @param out                Output target
     * @param predicate Optional Predicate for filtering headings
     */
    private void loadHeadings(SolrFieldIterator fieldIterator,
                              PrintWriter out,
                              Predicate predicate)
    throws Exception
    {
        for (BrowseEntry h : fieldIterator) {
            // We use a byte array for the sort key instead of a string to ensure
            // consistent sorting even if the index tool and browse handler are running
            // with different locale settings. Using strings results in less predictable
            // behavior.
            byte[] sort_key = h.key;
            String key_text = h.key_text;
            String heading = h.value;

            if (predicate != null &&
                    !predicate.isSatisfiedBy(heading)) {
                continue;
            }

            if (sort_key != null) {
                // Output a delimited key/value pair, base64-encoding both strings
                // to ensure that no characters overlap with the delimiter or introduce
                // \n's that could interfere with line-based sorting of the file.
                out.print(new String(Base64.encodeBase64(sort_key)) +
                          KEY_SEPARATOR +
                          new String(Base64.encodeBase64(key_text.getBytes(Charset.forName("UTF-8")))) +
                          KEY_SEPARATOR +
                          new String(Base64.encodeBase64(heading.getBytes(Charset.forName("UTF-8")))) +
                          RECORD_SEPARATOR);
            }
        }
    }


    private int bibCount(String heading) throws IOException
    {
        TotalHitCountCollector counter = new TotalHitCountCollector();

        bibSearcher.search(new ConstantScoreQuery(new TermQuery(new Term(luceneField, heading))),
                           counter);

        return counter.getTotalHits();
    }


    private boolean isLinkedFromBibData(String heading)
    throws IOException
    {
        TopDocs hits = null;

        int max_headings = 20;
        while (true) {
            hits = authSearcher.search
                   (new ConstantScoreQuery
                    (new TermQuery
                     (new Term
                      (System.getProperty("field.insteadof", "insteadOf"),
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

        StoredFields storedFields = authSearcher.getIndexReader().storedFields();
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            Document doc = storedFields.document(hits.scoreDocs[i].doc);

            String[] preferred = doc.getValues(System.getProperty("field.preferred", "preferred"));
            if (preferred.length > 0) {
                String preferredHeading = preferred[0];

                if (bibCount(preferredHeading) > 0) {
                    return true;
                }
            } else {
                return false;
            }
        }

        return false;
    }


    private SolrFieldIterator getBibIterator(String bibPath, String luceneField)
        throws Exception
    {
        String fieldIteratorClass = "org.vufind.solr.indexing.SolrFieldIterator";

        if (Utils.getEnvironment("BIBLEECH") != null) {
            if (System.getenv("BIBLEECH") != null) {
                Utils.printDeprecationWarning("You are using the 'BIBLEECH' environment variable.",
                                              "This still works, but it has been renamed to 'BIB_FIELD_ITERATOR'",
                                              "You should switch to avoid breakage in future versions.");
            }

            if (System.getProperty("bibleech") != null) {
                Utils.printDeprecationWarning("You are using the 'bibleech' system property.",
                                              "This still works, but it has been renamed to 'bib_field_iterator'",
                                              "You should switch to avoid breakage in future versions.");

            }

            fieldIteratorClass = Utils.getEnvironment("BIBLEECH");
        }


        if (Utils.getEnvironment("BIB_FIELD_ITERATOR") != null) {
            fieldIteratorClass = Utils.getEnvironment("BIB_FIELD_ITERATOR");
        }

        if ("StoredFieldLeech".equals(fieldIteratorClass)) {
            Utils.printDeprecationWarning("You are using the 'StoredFieldLeech' class.",
                                          "This still works, but it has been renamed to 'org.vufind.solr.indexing.StoredFieldIterator'",
                                          "You should switch to avoid breakage in future versions.");
            fieldIteratorClass = "org.vufind.solr.indexing.StoredFieldIterator";
        }

        return (SolrFieldIterator)(Class.forName(fieldIteratorClass)
                       .getConstructor(String.class, String.class)
                       .newInstance(bibPath, luceneField));
    }


    public void create(String bibPath,
                       String luceneField,
                       String authPath,
                       String outFile)
    throws Exception
    {
        try (SolrFieldIterator bibFieldIterator = getBibIterator(bibPath, luceneField)) {
            this.luceneField = luceneField;

            IndexReader bibReader = DirectoryReader.open(FSDirectory.open(new File(bibPath).toPath()));
            bibSearcher = new IndexSearcher(bibReader);

            try (PrintWriter out = new PrintWriter(new FileWriter(outFile))) {
                if (authPath != null) {
                    try {
                        nonprefAuthFieldIterator = new SolrFieldIterator(authPath,
                                                                         System.getProperty("field.insteadof",
                                                                                            "insteadOf"));
                    } catch (IndexNotFoundException e) {
                        // If no data has been written to the index yet, this exception
                        // might get thrown; in that case, we should skip loading authority
                        // data rather than breaking the whole indexing process.
                        nonprefAuthFieldIterator = null;
                    }

                    if (nonprefAuthFieldIterator != null) {
                        IndexReader authReader = DirectoryReader.open(FSDirectory.open(new File(authPath).toPath()));
                        authSearcher = new IndexSearcher(authReader);

                        loadHeadings(nonprefAuthFieldIterator, out,
                                     new Predicate() {
                                         public boolean isSatisfiedBy(Object obj) {
                                             String heading = (String) obj;

                                             try {
                                                 return isLinkedFromBibData(heading);
                                             } catch (IOException e) {
                                                 return true;
                                             }
                                         }
                                     }
                                     );

                        nonprefAuthFieldIterator.close();
                    }
                }

                loadHeadings(bibFieldIterator, out, null);
            }
        }
    }


    public static void main(String args[])
    throws Exception
    {
        if (args.length != 3 && args.length != 4) {
            System.err.println
            ("Usage: PrintBrowseHeadings <bib index> <bib field> "
             + "<auth index> <out file>");
            System.err.println("\nor:\n");
            System.err.println
            ("Usage: PrintBrowseHeadings <bib index> <bib field>"
             + " <out file>");

            System.exit(0);
        }

        PrintBrowseHeadings self = new PrintBrowseHeadings();

        if (args.length == 4) {
            self.create(args[0], args[1], args[2], args[3]);
        } else {
            self.create(args[0], args[1], null, args[2]);
        }
    }
}
