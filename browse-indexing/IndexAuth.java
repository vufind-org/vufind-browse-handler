import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.standard.*;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.*;


class IndexAuth
{
    private static Pattern trailingPunctuationRegexp =
        Pattern.compile ("[, ./;]+$");

    private static Pattern leadingPunctuationRegexp =
        Pattern.compile ("^[, ./;]+");


    private static String clean (String text)
    {
        String s = text;

        s = trailingPunctuationRegexp.matcher (s).replaceAll ("");
        s = leadingPunctuationRegexp.matcher (s).replaceAll ("");

        return s;
    }


    public static void main (String args[]) throws Exception
    {
        String dataFile = args[0];
        String indexDir = args[1];

        InputStream in = new FileInputStream (dataFile);
        MarcReader reader = new MarcStreamReader (in);

        StandardAnalyzer analyzer = (new StandardAnalyzer
                                     (org.apache.lucene.util.Version.LUCENE_41));

        IndexWriter iw = new IndexWriter (FSDirectory.open (new File (indexDir)),
                                          new IndexWriterConfig(org.apache.lucene.util.Version.LUCENE_41,
                                                                analyzer));

        while (reader.hasNext ()) {
            Document doc = new Document ();
            Record record = reader.next ();

            @SuppressWarnings("unchecked")
            List<DataField> fields = record.getDataFields ();

            for (DataField f : fields) {
                String field = null;

                if (f.getTag ().matches ("^1..$")) {
                    field = "preferred";
                } else if (f.getTag ().matches ("^4..$")) {
                    field = "insteadOf";
                } else if (f.getTag ().matches ("^5..$")) {
                    field = "seeAlso";
                } else if (f.getTag ().matches ("^(665|663|360)$")) {
                    field = "scopenote";
                }

                if (field != null) {
                    StringBuffer sb = new StringBuffer ();

                    List subfields = f.getSubfields ();
                    Iterator i = subfields.iterator ();

                    while (i.hasNext ()) {
                        Subfield subfield = (Subfield) i.next ();

                        if (subfield.getCode () == 'w') {
                            continue;
                        }

                        if (subfield.getCode () == 'v' ||
                            subfield.getCode () == 'x' ||
                            subfield.getCode () == 'y' ||
                            subfield.getCode () == 'z') {
                            sb.append ("-- ");
                        }

                        sb.append (subfield.getData ());
                        sb.append (" ");
                    }

                    doc.add (new Field (field,
                                        clean (sb.toString ()),
                                        StringField.TYPE_STORED));
                }
            }

            doc.add (new Field ("collection",
                                "Authority",
                                StringField.TYPE_NOT_STORED));

            iw.addDocument (doc);
        }

        iw.close ();
    }
}
