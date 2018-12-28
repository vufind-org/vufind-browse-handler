package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Store a list of items returned by a browse, along with a count of how far this
 * browse is positioned from the end of the index.
 */
@SuppressWarnings("serial")
public class BrowseList extends ArrayList<BrowseItem>
{
    /**
     * The remaining number of headings in the index after the current point in the
     * index.
     * <p>
     * The number of headings after the point your browse dropped you. So if you're
     * browsing a list of 100 headings and your results position you at 75,
     * {@literal totalCount} would be 25. It is used for pagination, so if you're at
     * position 75 with a page size of 20, you'll need a next page to see the last 5
     * headings.
     */
    public int totalCount = 0;

    public BrowseList()
    {
        super();
    }

    /**
     * Construct a BrowseList from a list of maps that represent browse items.
     *
     * @param list
     * @param totalCount
     */
    public BrowseList(List<Map<String, Object>> list, int totalCount)
    {
        super();
        for (Map<String, Object> item : list) {
            this.add(new BrowseItem(item));
        }
        this.totalCount = totalCount;
    }
}
