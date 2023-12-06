package org.vufind.solr.handler;

import java.util.Arrays;
import java.util.Map;

import org.vufind.util.Normalizer;

public class MatchTypeResponse
{
    private String from;
    private BrowseList results;
    private int rows, offset, rowid;
    private Normalizer normalizer;

    public MatchTypeResponse(String from, BrowseList results, int rowid, int rows, int offset, Normalizer normalizer)
    {
        this.from = from;
        this.results = results;
        this.rows = rows;
        this.rowid = rowid;
        this.offset = offset;
        this.normalizer = normalizer;
    }


    public enum MatchType {
        EXACT,
        HEAD_OF_STRING,
        NONE
    };


    private MatchType calculateMatchType(String heading, String query)
    {
        byte[] normalizedQuery = normalizer.normalize(query);
        byte[] normalizedHeading = normalizer.normalize(heading);

        if (Arrays.equals(normalizedQuery, normalizedHeading)) {
            return MatchType.EXACT;
        }

        if (heading.length() > 0) {
            for (int i = 1; i < heading.length(); i++) {
                byte[] normalizedHeadingPrefix = normalizer.normalize(heading.substring(0, i));
                if (Arrays.equals(normalizedQuery, normalizedHeadingPrefix)) {
                    return MatchType.HEAD_OF_STRING;
                }
            }
        }

        return MatchType.NONE;
    }


    public void addTo(Map<String,Object> solrResponse)
    {

        // Assume no match
        solrResponse.put("matchType", MatchType.NONE.toString());

        if (rows == 0) {
            return;
        }

        int adjustedOffset = offset;

        if ((rowid + offset) < 1) {
            // Our (negative) offset points before the beginning of the browse
            // list.  Set it to point to the beginning of the browse list.
            int distanceToStartOfBrowseList = rowid - 1;
            adjustedOffset = Math.max(adjustedOffset, -distanceToStartOfBrowseList);
        }

        if (from == null || "".equals(from)) {
            return;
        }

        if (adjustedOffset < 0 && (adjustedOffset + rows) <= 0) {
            // We're on a page before our matched heading
            return;
        }

        if (adjustedOffset > 0) {
            // We're on a page after our matched heading
            return;
        }

        if (results.isEmpty()) {
            // No results
            return;
        }

        int matched_item_index = Math.min(Math.abs(adjustedOffset),
                                          results.size() - 1);


        BrowseItem matched_item = results.get(matched_item_index);
        String matched_heading = matched_item.getSortKey();
        solrResponse.put("matchType", calculateMatchType(matched_heading, from).toString());
    }
}

