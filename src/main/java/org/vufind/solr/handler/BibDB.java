package org.vufind.solr.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

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
        return db.count(new TermQuery(new Term(field, heading)));
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

        int maxHits = (maxBibListSize > 0) ? maxBibListSize : db.count(q);
        if (maxHits > 0) {
            TopDocs docs = db.search(q, maxHits);

            try {
                for (ScoreDoc sd : docs.scoreDocs) {
                    Document doc = db.getIndexReader().storedFields().document(sd.doc);
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
                }
            } catch (org.apache.lucene.index.CorruptIndexException e) {
                Log.info("CORRUPT INDEX EXCEPTION.  EEK! - " + e);
            } catch (Exception e) {
                Log.info("Exception thrown: " + e);
            }
        }

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

        int maxHits = (maxBibListSize > 0) ? maxBibListSize : db.count(q);
        if (maxHits > 0) {
            TopDocs docs = db.search(q, maxHits);

            for (ScoreDoc sd : docs.scoreDocs) {
                try {
                    Document doc = db.getIndexReader().storedFields().document(sd.doc);
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
        }

        return bibinfo;
    }
}
