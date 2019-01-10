package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Class that performs the alphabetical browse of an index and produces a
 * {@code BrowseList} object.
 *
 */
class Browse
{
    private HeadingsDB headingsDB;
    private AuthDB authDB;
    private BibDB bibDB;
    private int maxBibListSize;

    public Browse(HeadingsDB headings, BibDB bibdb, AuthDB auth,
                  boolean retrieveBibId, int maxBibListSize)
    {
        this.headingsDB = headings;
        this.authDB = auth;
        this.bibDB = bibdb;
        this.maxBibListSize = maxBibListSize;
    }

    private void populateItem(BrowseItem item, String fields) throws Exception
    {
        Map<String, List<Collection<String>>> bibinfo =
            bibDB.matchingExtras(item.getHeading(), fields, maxBibListSize);
        item.setExtras(bibinfo);
        item.setCount(bibDB.recordCount(item.getHeading()));

        Map<String, List<String>> authFields = authDB.getFields(item.getHeading());

        List<String> seeAlsoList = new ArrayList<String>();
        for (String value : authFields.get("seeAlso")) {
            if (bibDB.recordCount(value) > 0) {
                seeAlsoList.add(value);
            }
        }
        item.setSeeAlso(seeAlsoList);

        List<String> useInsteadList = new ArrayList<String>();
        for (String value : authFields.get("useInstead")) {
            if (bibDB.recordCount(value) > 0) {
                useInsteadList.add(value);
            }
        }
        item.setUseInstead(useInsteadList);

        for (String value : authFields.get("note")) {
            item.setNote(value);
        }
    }


    public int getId(String from) throws Exception
    {
        return headingsDB.getHeadingStart(from);
    }


    public BrowseList getList(int rowid, int offset, int rows, String extras)
    throws Exception
    {
        BrowseList result = new BrowseList();

        HeadingSlice h = headingsDB.getHeadings(Math.max(1, rowid + offset),
                                                rows);

        result.totalCount = h.total;

        for (int i = 0; i < h.headings.size(); i++) {
            String heading = h.headings.get(i);
            String sort_key = h.sort_keys.get(i);

            BrowseItem item = new BrowseItem(sort_key, heading);

            populateItem(item, extras);

            result.add(item);
        }

        return result;
    }
}