package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class BrowseList extends ArrayList<BrowseItem>
{
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
