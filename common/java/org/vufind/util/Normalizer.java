package org.vufind.util;

/**
 * An interface for alphabetical browse normalizers.
 *
 * @author Tod Olson <tod@uchicago.edu>
 *
 */

public interface Normalizer
{

    /**
     * Normalizes the input string to a collation key, which will be used for the
     * sort order of the browse index.
     *
     * We use a byte array for the sort key instead of a string to ensure consistent
     * sorting even if the index tool and browse handler are running with different
     * locale settings. Using strings results in less predictable behavior.
     *
     * @param s the string to be normalized, e.g. a term for the browse index
     * @return	collation key for determining browse order
     */
    public byte[] normalize(String s);

}