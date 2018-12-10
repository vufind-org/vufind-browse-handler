package org.vufind.solr.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;

/**
 *
 * Interface to the Solr biblio db
 *
 */
public class BibDB
{
    private IndexSearcher db;
    private String field;

    public BibDB(IndexSearcher searcher, String field)
    {
        this.db = searcher;
        this.field = field;
    }

    /**
     * Returns the number of bib records that match an authority heading.
     * 
     * @param heading
     * @return	number of matching bib records
     * @throws Exception
     */
    public int recordCount(String heading)
    throws IOException
    {
        TermQuery q = new TermQuery(new Term(field, heading));

        Log.info("Searching '" + field + "' for '" + heading + "'");

        TotalHitCountCollector counter = new TotalHitCountCollector();
        db.search(q, counter);

        Log.info("Hits: " + counter.getTotalHits());

        return counter.getTotalHits();
    }


    /**
     *
     * Function to retrieve the doc ids when there is a building limit
     * This retrieves the doc ids for an individual heading
     *
     * Need to add a filter query to limit the results from Solr
     *
     * Includes functionality to retrieve additional info
     * like titles for call numbers, possibly ISBNs
     *
     * @param heading        string of the heading to use for finding matching
     * @param fields         docs colon-separated string of Solr fields
     *                       to return for use in the browse display
     * @param maxBibListSize maximum numbers of records to check for fields
     * @return         return a map of Solr ids and extra bib info
     */
    public Map<String, List<Collection<String>>> matchingIDs(String heading, 
                                                             String fields,
                                                             int maxBibListSize)
    throws Exception
    {
        TermQuery q = new TermQuery(new Term(field, heading));

        // bibinfo values are List<Collection> because some extra fields
        // may be multi-valued.
        // Note: it may be time for bibinfo to become a class...
        final Map<String, List<Collection<String>>> bibinfo = new HashMap<> ();
        // Forcing "ids" into list of bib fields is a transition to requiring
        // that "ids" be listed explicitly in the extras string
        final String[] bibFieldList = fields.split(":");
        for (String bibField : bibFieldList) {
            bibinfo.put(bibField, new ArrayList<Collection<String>> ());
        }

        db.search(q, new SimpleCollector() {
            private LeafReaderContext context;

            public void setScorer(Scorer scorer) {
            }

            // Will only be used by other classes
            @SuppressWarnings("unused")
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

                    for (String bibField : bibFieldList) {
                        String[] vals = doc.getValues(bibField);
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
