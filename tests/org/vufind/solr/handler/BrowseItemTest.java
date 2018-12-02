package org.vufind.solr.handler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class BrowseItemTest
{

    @Test
    public void testBrowseItem()
    {
        String heading = "Test Heading";
        String sort_key = heading.toLowerCase();
        BrowseItem item = new BrowseItem(sort_key, heading);
        assertEquals(sort_key, item.get("sort_key"));
        assertEquals(heading, item.get("heading"));
    }

    @Test
    public void testSetSortKey()
    {
        String heading = "Test Heading";
        String sort_key = heading.toLowerCase();
        BrowseItem item = new BrowseItem("", "");
        item.setSortKey(sort_key);
        assertEquals(sort_key, item.get("sort_key"));
    }

    @Test
    public void testSetHeading()
    {
        String heading = "Test Heading";
        BrowseItem item = new BrowseItem("", "");
        item.setHeading(heading);
        assertEquals(heading, item.get("heading"));
    }

    @Test
    public void testSetSeeAlso()
    {
        List<String> seeAlso = new ArrayList<String>();
        seeAlso.add("Test See Also");
        seeAlso.add("Test See Yet Another");
        BrowseItem item = new BrowseItem("", "");
        item.setSeeAlso(seeAlso);
        assertEquals(seeAlso, item.get("seeAlso"));
    }

    @Test
    public void testSetUseInstead()
    {
        List<String> useInstead = new ArrayList<String>();
        useInstead.add("Use Instead");
        useInstead.add("Use this too");
        BrowseItem item = new BrowseItem("", "");
        item.setUseInstead(useInstead);
        assertEquals(useInstead, item.get("useInstead"));
    }

    @Test
    public void testSetNote()
    {
        String note = "Test Note";
        BrowseItem item = new BrowseItem("", "");
        item.setNote(note);
        assertEquals(note, item.get("note"));
    }

    @Test
    public void testSetIds()
    {
        Collection<String> ids1 = new ArrayList<String>();
        ids1.add("id-1");
        Collection<String> ids2 = new ArrayList<String>();
        ids1.add("id-2");
        // This is what we expect to store, a list containing all IDs
        List<String> allIds = new ArrayList<String>();
        allIds.addAll(ids1);
        allIds.addAll(ids2);

        // This is what setIds expects, a list of collections
        List<Collection<String>> idList = new ArrayList<Collection<String>>();
        idList.add(ids1);
        idList.add(ids2);

        BrowseItem item = new BrowseItem("", "");
        item.setIds(idList);

        // IDs are stored as the concatenation of the list of collections
        assertEquals(allIds, item.get("ids"));
    }

    @Ignore
    @Test
    public void testSetFields()
    {
        fail("Not yet implemented");
    }

    @Test
    public void testSetCountInt()
    {
        int count = 37;
        BrowseItem item = new BrowseItem("", "");
        item.setCount(count);
        assertEquals(new Integer(count), item.get("count"));
    }

    @Test
    public void testSetCountInteger()
    {
        Integer count = new Integer(87);
        BrowseItem item = new BrowseItem("", "");
        item.setCount(count);
        assertEquals(count, item.get("count"));
    }

    @Test
    public void testGetHeading()
    {
        String heading = "Test Heading";
        String sort_key = heading.toLowerCase();
        BrowseItem item = new BrowseItem(sort_key, heading);
        assertEquals(heading, item.getHeading());
    }

    @Test
    public void testGetSortKey()
    {
        String heading = "Test Heading";
        String sort_key = heading.toLowerCase();
        BrowseItem item = new BrowseItem(sort_key, heading);
        assertEquals(sort_key, item.getSortKey());
    }

    @Test
    public void testGetSeeAlso()
    {
        List<String> seeAlso = new ArrayList<String>();
        seeAlso.add("Test See Also");
        seeAlso.add("Test See Yet Another");
        BrowseItem item = new BrowseItem("", "");
        item.setSeeAlso(seeAlso);
        assertEquals(seeAlso, item.getSeeAlso());
    }

    @Test
    public void testGetUseInstead()
    {
        List<String> useInstead = new ArrayList<String>();
        useInstead.add("Use Instead");
        useInstead.add("Use this too");
        BrowseItem item = new BrowseItem("", "");
        item.setUseInstead(useInstead);
        assertEquals(useInstead, item.getUseInstead());
    }

    @Test
    public void testGetNote()
    {
        String note = "Test Note";
        BrowseItem item = new BrowseItem("", "");
        item.setNote(note);
        assertEquals(note, item.getNote());
    }

    @Test
    public void testGetIds()
    {
        Collection<String> ids1 = new ArrayList<String>();
        ids1.add("id-1");
        Collection<String> ids2 = new ArrayList<String>();
        ids1.add("id-2");
        // This is what we expect to store, a list containing all IDs
        List<String> allIds = new ArrayList<String>();
        allIds.addAll(ids1);
        allIds.addAll(ids2);

        // This is what setIds expects, a list of collections
        List<Collection<String>> idList = new ArrayList<Collection<String>>();
        idList.add(ids1);
        idList.add(ids2);

        BrowseItem item = new BrowseItem("", "");
        item.setIds(idList);

        // IDs are stored as the concatenation of the list of collections
        assertEquals(allIds, item.getIds());
    }

    @Ignore
    @Test
    public void testGetFields()
    {
        fail("Not yet implemented");
    }

    @Test
    public void testGetCount()
    {
        Integer count = new Integer(87);
        BrowseItem item = new BrowseItem("", "");
        item.setCount(count);
        assertEquals(count, item.getCount());
    }

    @Test
    public void testGetCountAsInt()
    {
        int count = 37;
        BrowseItem item = new BrowseItem("", "");
        item.setCount(count);
        assertEquals(count, item.getCountAsInt());
    }

    @Ignore
    @Test
    public void testPutStringObject()
    {
        fail("Not yet implemented");
    }

}
