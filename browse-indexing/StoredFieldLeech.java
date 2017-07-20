// Build a browse list by walking the docs in an index and extracting sort key
// and values from a pair of stored fields.

import java.io.*;
import java.util.*;
import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;

import org.vufind.util.Utils;
import org.vufind.util.BrowseEntry;

public class StoredFieldLeech extends Leech
{
    int currentDoc = 0;
    LinkedList<BrowseEntry> buffer;

    String sortField;
    String valueField;

    private Set<String> fieldSelection;


    public StoredFieldLeech(String indexPath, String field) throws Exception
    {
        super(indexPath, field);

        sortField = Utils.getEnvironment("SORTFIELD");
        valueField = Utils.getEnvironment("VALUEFIELD");

        if (sortField == null || valueField == null) {
            throw new IllegalArgumentException("Both SORTFIELD and " +
                                               "VALUEFIELD environment " +
                                               "variables must be set.");
        }

        fieldSelection = new HashSet<String>();
        fieldSelection.add(sortField);
        fieldSelection.add(valueField);
        fieldSelection.add("id");   // make Solr id available for error messages

        reader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));
        buffer = new LinkedList<BrowseEntry> ();
    }


    private void loadDocument(IndexReader reader, int docid)
    throws Exception
    {
        Document doc = reader.document(currentDoc, fieldSelection);

        String[] sort_key = doc.getValues(sortField);
        String[] value = doc.getValues(valueField);

        if (sort_key.length == value.length) {
            for (int i = 0; i < value.length; i++) {
                buffer.add(new BrowseEntry(buildSortKey(sort_key[i]),
                                           sort_key[i],
                                           value[i]));
            }
        } else {
            String id = null;
            IndexableField idField = doc.getField("id");
            if (idField != null) {
                /*
                 * Assumes id is defined as type string in Solr schema.
                 * Should be safe for VuFind.
                 */
                id = idField.stringValue();
            }
            System.err.println("Skipped entries for doc #" + docid +
                               " (id:" + id + "):" +
                               " the number of sort keys didn't" +
                               " match the number of stored values.");
        }
    }


    public BrowseEntry next() throws Exception
    {
        while (buffer.isEmpty()) {
            if (currentDoc < reader.maxDoc()) {
                loadDocument(reader, currentDoc);
                currentDoc++;
            } else {
                return null;
            }
        }

        return buffer.remove();
    }
}

