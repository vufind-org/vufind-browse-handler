package org.vufind.solr.browse.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.vufind.util.DeweyCallNormalizer;

public class DeweyCallNumberNormalizerTest
{
    private DeweyCallNormalizer deweyNormalizer;

    @Before
    public void setUp() {
        deweyNormalizer = new DeweyCallNormalizer ();
    }

    @Test
    public void sortsDewey() {
        assertEquals(Helpers.listOf("1.23 .I39", "111.123 I39", 
        		"322.45 .R513 1957", "324.54 .I39 F", "324.548 .C425R", "324.6 .A75CUA"),
        		Helpers.sort(deweyNormalizer,
        				Helpers.listOf("324.548 .C425R", "322.45 .R513 1957", 
        						"324.6 .A75CUA", "1.23 .I39", "111.123 I39", "324.54 .I39 F")));
    }

}
