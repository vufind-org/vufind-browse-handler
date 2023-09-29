package org.vufind.solr.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;

/**
 *
 * Interface to the Solr biblio db.
 * <p>
 * This class provides a way to look up headings in one single field of the
 * bibilio core, specified in the constructor.
 *
 */
public class BibDB
{
    private IndexSearcher db;
    private String field;

    /**
     * @param searcher an index searcher connected to the bibilio core.
     * @param field    the field that will be searched for matching headings.
     */
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

        TotalHitCountCollector counter = new TotalHitCountCollector();
        db.search(q, counter);

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
     * @param extras         docs colon-separated string of Solr fields
     *                       to return for use in the browse display
     * @param maxBibListSize maximum numbers of records to check for fields
     * @return         return a map of Solr ids and extra bib info
     */
    @Deprecated
    public Map<String, List<Collection<String>>> matchingIDs(String heading,
            String extras,
            int maxBibListSize)
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

            // Will only be used by other classes
            @SuppressWarnings("unused")
            public boolean acceptsDocsOutOfOrder() {
                return true;
            }

            public boolean needsScores() {
                return false;
            }

            public ScoreMode scoreMode() {
                return ScoreMode.COMPLETE_NO_SCORES;
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

    /**
     * Function to retrieve the extra fields needed for building the browse display.
     * <p>
     * This method retrieves fields from all docs matching the heading. Will not make query
     * if {@code extras} is null or empty.
     * <p>
     * {@code maxBibListSize} puts a limit on how many documents will be consulted.
     * If {@code maxBibListSize} <= 0, there is no limit.
     * <p>
     * This method will be used for returning the extra fields in VuFind 5.1.
     *
     * @param heading        string of the heading to use for finding matching docs
     * @param extras         colon-separated string of Solr fields
     *                       to return for use in the browse display
     * @param maxBibListSize maximum numbers of records to check for fields
     * @return         return a map of extra bib info
     */
    public Map<String, List<Collection<String>>> matchingExtras(String heading,
            String extras,
            int maxBibListSize)
    throws Exception
    {
        // short circuit if we don't need to do any work
        if (extras == null || extras.isEmpty()) {
            return null;
        }

        TermQuery q = new TermQuery(new Term(this.field, heading));

        // bibinfo values are List<Collection> because some extra fields
        // may be multi-valued.
        // Note: keeping bibinfo as List<Collection> gives us free serializing for the eventual response.
        final Map<String, List<Collection<String>>> bibinfo = new HashMap<> ();
        final String[] bibExtras = extras.split(":");
        for (String bibField : bibExtras) {
            bibinfo.put(bibField, new ArrayList<Collection<String>> ());
        }

        db.search(q, new SimpleCollector() {
            private LeafReaderContext context;
            private int docCount = 0;

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

            public ScoreMode scoreMode() {
                return ScoreMode.COMPLETE_NO_SCORES;
            }

            public void doSetNextReader(LeafReaderContext context) {
                this.context = context;
            }


            public void collect(int docnum) {
                // Terminate collection if exceed maximum bibs
                if (maxBibListSize > 0 && this.docCount >= maxBibListSize) {
                    throw new CollectionTerminatedException();
                } else {
                    this.docCount++;
                }

                int docid = docnum + context.docBase;
                try {
                    Document doc = db.getIndexReader().storedFields().document(docid);
                    for (String bibField : bibExtras) {
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

    /**
     * Function to retrieve fields needed for building the browse display, beyond the heading itself.
     * <p>
     * This method retrieves fields from all docs matching the heading. Will not make query
     * if {@code extras} is null or empty.
     * <p>
     * {@code maxBibListSize} puts a limit on how many documents will be consulted.
     * If {@code maxBibListSize} <= 0, there is no limit.
     * <p>
     * This method will be used for returning the extra fields as of VuFind 6.0.
     *
     * @param heading        string of the heading to use for finding matching docs
     * @param fields         colon-separated string of Solr fields
     *                       to return for use in the browse display
     * @param maxBibListSize maximum numbers of records to check for fields
     * @return         return a map of exta bib field info
     */
    public Map<String, Collection<String>> matchingFields(String heading,
            String fields,
            int maxBibListSize)
    throws Exception
    {
        // short circuit if we don't need to do any work
        if (fields == null || fields.isEmpty()) {
            return null;
        }

        TermQuery q = new TermQuery(new Term(this.field, heading));

        // bibinfo values are List<Collection> because some extra fields
        // may be multi-valued.
        // Note: keeping bibinfo as Collection gives us free serializing for the eventual response.
        final Map<String, Collection<String>> bibinfo = new HashMap<> ();
        final String[] bibExtras = fields.split(":");
        for (String bibField : bibExtras) {
            bibinfo.put(bibField, new LinkedHashSet<String> ());
        }

        db.search(q, new SimpleCollector() {
            private LeafReaderContext context;
            private int docCount = 0;

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

            public ScoreMode scoreMode() {
                return ScoreMode.COMPLETE_NO_SCORES;
            }

            public void doSetNextReader(LeafReaderContext context) {
                this.context = context;
            }


            public void collect(int docnum) {
                // Terminate collection if exceed maximum bibs
                if (maxBibListSize > 0 && this.docCount >= maxBibListSize) {
                    throw new CollectionTerminatedException();
                } else {
                    this.docCount++;
                }

                int docid = docnum + context.docBase;
                try {
                    Document doc = db.getIndexReader().storedFields().document(docid);
                    for (String bibField : bibExtras) {
                        for (String v : doc.getValues(bibField)) {
                            bibinfo.get(bibField).add(v);
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
