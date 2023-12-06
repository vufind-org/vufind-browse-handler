package org.vufind.solr.handler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void testBrowsItem_MapConstructor()
    {
        String sortKey = "hobbit";
        String heading = "Hobbit";
        String random = "asdf";
        Map<String, Object> itemMap = new HashMap<>();
        itemMap.put("sort_key", sortKey);
        itemMap.put("heading", heading);
        itemMap.put("random", random);

        BrowseItem item = new BrowseItem(itemMap);
        assertEquals(sortKey, item.get("sort_key"));
        assertEquals(heading, item.get("heading"));
        assertEquals(random, item.get("random"));
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
    public void testSetExtras()
    {
        Collection<String> format1 = new ArrayList<>();
        format1.add("Print");
        Collection<String> format2 = new ArrayList<>();
        format2.add("e-Resource");

        List<Collection<String>> formatList = new ArrayList<>();
        formatList.add(format1);
        formatList.add(format2);

        Map<String, List<Collection<String>>> extras = new HashMap<String, List<Collection<String>>>();
        extras.put("format", formatList);

        Log.info("testSetExtras: extras = %s", extras);

        BrowseItem item = new BrowseItem("", "");
        item.setExtras(extras);
        assertEquals(extras, item.get("extras"));
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
        assertEquals(Integer.valueOf(count), item.get("count"));
    }

    @Test
    public void testSetCountInteger()
    {
        Integer count = Integer.valueOf(87);
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
    public void testGetExtras()
    {
        Collection<String> format1 = new ArrayList<>();
        format1.add("Book");
        Collection<String> format2 = new ArrayList<>();
        format2.add("Serial");

        List<Collection<String>> formatList = new ArrayList<>();
        formatList.add(format1);
        formatList.add(format2);

        Map<String, List<Collection<String>>> extras = new HashMap<String, List<Collection<String>>>();
        extras.put("format", formatList);

        BrowseItem item = new BrowseItem("", "");
        item.setExtras(extras);
        //Log.info("testGetExtras: %s", item.getExtras());
        assertEquals(extras, item.getExtras());
        assertEquals(formatList, extras.get("format"));
    }

    @Test
    public void testGetExtras_noExtras()
    {
        Map<String, List<Collection<String>>> extras = new HashMap<String, List<Collection<String>>>();

        BrowseItem item = new BrowseItem("", "");
        //Log.info("testGetExtras_noExtras: ", item.getExtras());
        assertEquals(extras, item.getExtras());
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
        Integer count = Integer.valueOf(87);
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
