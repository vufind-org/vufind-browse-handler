package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrowseList
{
    public int totalCount;
    public List<BrowseItem> items = new ArrayList<> ();


    public List<Map<String, Object>> asMap()
    {
        List<Map<String, Object>> result = new ArrayList<> ();

        for (BrowseItem item : items) {
            result.add(item.asMap());
        }

        return result;
    }
}
