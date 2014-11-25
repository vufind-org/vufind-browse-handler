package org.vufind.solr.browse.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import org.vufind.util.Normalizer;

/**
 * Helper methods for testing the browse normalizers.
 * 
 * @author Mark Triggs
 * @author Tod Olson <tod@uchicago.edu>
 *
 */
public class Helpers {

	/**
	 * Build a <code>List&lt;String&gt;</code> of from a variable number of arguments,
	 * in the order the arguments are listed. 
	 * 
	 * @param args
	 * @return     a list composed of the arguments
	 */
    public static List<String> listOf(String ... args) {
        List<String> result = new ArrayList<String>();
        for (String s : args) {
            result.add(s);
        }

        return result;
    }

    /**
     * Lexicographically compare two byte arrays. 
     * 
     * Algorithm found on StackOverflow and attributed to Apache HBase:
     * http://stackoverflow.com/questions/5108091/java-comparator-for-byte-array-lexicographic
     * 
     * @param left  first byte array to compare
     * @param right second byte array to compare
     * @return
     */
    public static int compareByteArrays(byte[] left, byte[] right) {
        for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
            int a = (left[i] & 0xff);
            int b = (right[j] & 0xff);
            if (a != b) {
                return a - b;
            }
        }
        return left.length - right.length;
    }

    /**
     * Sorts a list according to the sort tokens produced by the <code>Normalizer</code> object.
     * 
     * @param normalizer <code>Normalizer</code> to use for computing sort keys.
     * @param list       strings to sort
     * @return           strings sorted by their computed sort keys
     */
    public static List<String> sort (final Normalizer normalizer, List<String> list) {
        List<String> result = new ArrayList<String>();
        result.addAll(list);

        Collections.sort(result, new Comparator<String>() {
                public int compare(String s1, String s2) {
                    return compareByteArrays(normalizer.normalize(s1),
                                             normalizer.normalize(s2));
                }
            });

        return result;
    }
    
    /**
     * Dummy test so JUnit does not complain that there are no runnable tests for this class. 
     */
    @Test
    public void dummyTest() {
    	return;
    }

}
