package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class BrowseList extends ArrayList<BrowseItem>
{
    public int totalCount;
    // public List<BrowseItem> items = new ArrayList<> ();

    /*
    public List<BrowseItem> asMap() {
        return this.items;
    }
    */

    // Hopefully this is temporary and can make BrowseList extend something sensible
    /*
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // TODO: re-implement as a join, if we can figure that out...
        sb.append('[');
        int sz = items.size();
        for (int i=0; i<sz; i++) {
          if (i != 0) sb.append(',');
          sb.append(items.get(i));
        }
        sb.append(']');

        return sb.toString();
      }
      */
}
