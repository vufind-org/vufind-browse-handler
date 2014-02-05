package org.vufind.solr.browse.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.vufind.util.UChicagoLCCallNormalizer;

public class UChicagoLCCallNumberNormalizerTest
{
    private UChicagoLCCallNormalizer ucLCNormalizer;

    @Before
    public void setUp() {
        ucLCNormalizer = new UChicagoLCCallNormalizer ();
    }

    /*
     * Tests of UChicagoLCCallNumber#shelfKey are already pretty strenuous, so just sanity check here.
     */
    @Test
    public void sortsUChicagoLCClassification() {
        assertEquals(Helpers.listOf("AB9.22 L3", "ABR92.L3", "B82 L3", "B82.2 L3", "B822 L3", "B8220 L3"
        		, "KD671.G53", "XXKD671.G53", "XXKFA 207.A940"),
        		Helpers.sort(ucLCNormalizer,
        				Helpers.listOf("B822 L3", "XXKFA 207.A940", "B82 L3", "XXKD671.G53", 
        						"B8220 L3", "KD671.G53", "AB9.22 L3", "ABR92.L3", "B82.2 L3")));
    }

}
