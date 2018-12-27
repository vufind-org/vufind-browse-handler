package org.vufind.solr.handler;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.Map;
import java.util.HashMap;

import org.vufind.util.Normalizer;
import org.vufind.util.NormalizerFactory;


public class BrowseHighlightingTest
{

    @Test
    public void simpleHeadOfString() throws Exception
    {
        assertEquals("HEAD_OF_STRING",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry"), "app", 1, 20, 0));
    }


    @Test
    public void simpleExactMatch() throws Exception
    {
        assertEquals("EXACT",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry"), "apple", 1, 20, 0));
    }


    @Test
    public void simpleMismatch() throws Exception
    {
        assertEquals("NONE",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry"), "aardvark", 1, 20, 0));
    }


    @Test
    public void noRowsMeansNoMatch() throws Exception
    {
        assertEquals("NONE",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry"), "apple", 1, 0, 0));
    }


    @Test
    public void emptyQueryMeansNoMatch() throws Exception
    {
        assertEquals("NONE",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry"), "", 1, 20, 0));
    }


    @Test
    public void emptyResultsMeansNoMatch() throws Exception
    {
        assertEquals("NONE",
                     matchTypeFor(fakeBrowseResults(), "apple", 1, 20, 0));
    }

    @Test
    public void onlyShowMatchTypeForFirstPage() throws Exception
    {
        // Forward a page (matched on "apple")
        assertEquals("NONE",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry", "dates"), "apple", 1, 2, 2));

        // Back a page (matched on "cherry")
        assertEquals("NONE",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry", "dates"), "cherry", 3, 2, -2));

        // Back a little, but match still visible
        assertEquals("EXACT",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry", "dates"), "dates", 4, 10, -3));

    }


    @Test
    public void negativeOffsetAtStartOfBrowseList() throws Exception
    {
        // Offset of -2 is effectively ignored since we're at the start of the
        // browse list.  The heading is still visible, so we should show the
        // exact match.
        assertEquals("EXACT",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry", "dates"), "apple", 1, 2, -2));

        assertEquals("HEAD_OF_STRING",
                     matchTypeFor(fakeBrowseResults("apple", "banana"), "b", 2, 2, -2));
    }


    @Test
    public void offsetPastEndOfBrowseList() throws Exception
    {
        // If our offset moves us past the end of the browse list, there are no
        // results and we show no match.
        assertEquals("NONE",
                     matchTypeFor(fakeBrowseResults("apple", "banana", "cherry", "dates"), "dates", 4, 2, 100));
    }



    // Helpers

    private String matchTypeFor(BrowseList browseList, String query, int rowid, int rows, int offset)
    throws Exception
    {
        MatchTypeResponse mtr = new MatchTypeResponse(query, browseList, rowid, rows, offset, getNormalizer());

        Map<String, Object> solrResponse = fakeSolrResponse();

        mtr.addTo(solrResponse);

        return (String)solrResponse.get("matchType");
    }


    private Map<String, Object> fakeSolrResponse()
    {
        return new HashMap<String, Object>();
    }

    private Normalizer getNormalizer() throws Exception
    {
        return NormalizerFactory.getNormalizer("org.vufind.util.ICUCollatorNormalizer");
    }


    private BrowseList fakeBrowseItems(String[] headings)
    {
        BrowseList result = new BrowseList();

        for (String heading : headings) {
            result.add(new BrowseItem(heading, heading));
        }

        return result;
    }


    private BrowseList fakeBrowseResults(String ... headings)
    {
        BrowseList result = new BrowseList();
        result = fakeBrowseItems(headings);
        return result;
    }

}
