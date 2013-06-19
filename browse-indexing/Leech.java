import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import java.io.*;

import org.vufind.util.Normaliser;
import org.vufind.util.BrowseEntry;


public class Leech
{
    protected IndexReader reader;
    protected IndexSearcher searcher;

    private String field;
    private TermEnum tenum;
    private Normaliser normaliser;


    public Leech (String indexPath,
                  String field) throws Exception
    {
        reader = IndexReader.open (FSDirectory.open (new File (indexPath)));
        searcher = new IndexSearcher (reader);
        this.field = field;
        tenum = reader.terms (new Term (field, ""));

        normaliser = Normaliser.getInstance ();
    }


    public byte[] buildSortKey (String heading)
    {
        return normaliser.normalise (heading);
    }


    public void dropOff () throws IOException
    {
        searcher.close ();
        reader.close ();
    }


    private boolean termExists (Term t)
    {
        try {
            return (this.searcher.search (new ConstantScoreQuery(new TermQuery (t)),
                                          1).totalHits > 0);
        } catch (IOException e) {
            return false;
        }
    }


    public BrowseEntry next () throws Exception
    {
        if (tenum.term () != null &&
            tenum.term ().field ().equals (this.field)) {
            if (termExists (tenum.term ())) {
                String term = tenum.term ().text ();
                tenum.next ();
                return new BrowseEntry (buildSortKey (term), term);
            } else {
                tenum.next ();
                return this.next ();
            }
        } else {
            return null;
        }
    }
}
