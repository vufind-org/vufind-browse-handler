package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulate a slice of a headings index.
 *
 */
class HeadingSlice
{
    public List<String> sort_keys = new ArrayList<> ();
    public List<String> headings = new ArrayList<> ();
    /**
     * Offset from beginning of index.
     * <p>
     * See source for {@link HeadingsDB#getHeadings(int, int)}
     */
    public int total;
}