package org.vufind.util;

import java.io.PrintStream;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

public class Utils
{
    public static String getEnvironment (String var)
    {
        return (System.getenv (var) != null) ?
            System.getenv (var) : System.getProperty (var.toLowerCase ());
    }
    
    /**
     * Dump the documement fields as human-readable field/value pairs form.
     * Intended for debugging use.
     * 
     * @param doc    Solr document that one wishes to examine.
     * @param stream where to write the 
     */
    public static void dumpDocument (Document doc, PrintStream stream) {
        List<IndexableField> fields = doc.getFields();
        stream.println("Document dump:");
        for (IndexableField f : fields) {
        	String name = f.name();
        	String val = f.stringValue();
        	if (val == null) {
        		val = f.numericValue().toString();
        	}
        	stream.println(name + "=" + val);
        }
    }
}
