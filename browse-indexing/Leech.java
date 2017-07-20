import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import java.io.*;
import java.util.*;

import org.vufind.util.BrowseEntry;
import org.vufind.util.Normalizer;
import org.vufind.util.NormalizerFactory;


public class Leech
{
    protected CompositeReader reader;
    protected IndexSearcher searcher;

    protected List<LeafReaderContext> leafReaders;

    private String field;
    private Normalizer normalizer;

    TermsEnum tenum = null;


    public Leech(String indexPath,
                 String field) throws Exception
    {
        // Open our composite reader (a top-level DirectoryReader that
        // contains one reader per segment in our index).
        reader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));

        // Open the searcher that we'll use to verify that items are
        // being used by a non-deleted document.
        searcher = new IndexSearcher(reader);

        // Extract the list of readers for our underlying segments.
        // We'll work through these one at a time until we've consumed them all.
        leafReaders = new ArrayList<>(reader.getContext().leaves());

        this.field = field;

        String normalizerClass = System.getProperty("browse.normalizer");
        normalizer = NormalizerFactory.getNormalizer(normalizerClass);
    }


    public byte[] buildSortKey(String heading)
    {
        return normalizer.normalize(heading);
    }


    public void dropOff() throws IOException
    {
        reader.close();
    }


    private boolean termExists(String t)
    {
        try {
            return (this.searcher.search(new ConstantScoreQuery(new TermQuery(new Term(this.field, t))),
                                         1).totalHits > 0);
        } catch (IOException e) {
            return false;
        }
    }


    // Return the next term from the currently selected TermEnum, if there is one.  Null otherwise.
    //
    // If there's no currently selected TermEnum, create one from the reader.
    //
    public BrowseEntry next() throws Exception
    {
        if (tenum == null) {
            if (leafReaders.isEmpty()) {
                // Nothing left to do
                return null;
            }

            // Select our next LeafReader to work from
            LeafReader ir = leafReaders.remove(0).reader();
            Terms terms = ir.terms(this.field);

            if (terms == null) {
                // Try the next reader
                return next();
            }

            tenum = terms.iterator();
        }

        if (tenum.next() != null) {
            String termText = tenum.term().utf8ToString();

            if (termExists(termText)) {
                return new BrowseEntry(buildSortKey(termText), termText, termText) ;
            } else {
                return this.next();
            }
        } else {
            // Exhausted this reader
            tenum = null;
            return null;
        }
    }
}
