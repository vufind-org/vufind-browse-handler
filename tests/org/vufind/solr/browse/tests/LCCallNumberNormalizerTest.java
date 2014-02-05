package org.vufind.solr.browse.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.vufind.util.LCCallNormalizer;

public class LCCallNumberNormalizerTest
{
    private LCCallNormalizer lcNormalizer;

    @Before
    public void setUp() {
        lcNormalizer = new LCCallNormalizer ();
    }

    /*
     * Tests of LCCallNumber#shelfKey are already pretty strenuous, so just sanity check here.
     */
    @Test
    public void sortsLCClassification() {
        assertEquals(Helpers.listOf("AB9.22 L3", "ABR92.L3", "B82 L3", "B82.2 L3", "B822 L3", "B8220 L3"),
        		Helpers.sort(lcNormalizer,
        				Helpers.listOf("B822 L3", "B82 L3", "B8220 L3", "AB9.22 L3", "ABR92.L3", "B82.2 L3")));
    }

}
