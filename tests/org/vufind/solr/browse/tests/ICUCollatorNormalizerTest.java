package org.vufind.solr.browse.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.vufind.util.ICUCollatorNormalizer;

public class ICUCollatorNormalizerTest
{
    private ICUCollatorNormalizer iCUCollatorNormalizer;

    @Before
    public void setUp() {
        iCUCollatorNormalizer = new ICUCollatorNormalizer ();
    }


    @Test
    public void sortsSimpleStrings() {
        assertEquals(Helpers.listOf("apple", "banana", "cherry", "orange"),
        		Helpers.sort(iCUCollatorNormalizer,
        				Helpers.listOf ("banana", "orange", "apple", "cherry")));
    }


    @Test
    public void sortsDiacriticStrings() {
        assertEquals(Helpers.listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit", "Orange"),
        		Helpers.sort(iCUCollatorNormalizer,
        				Helpers.listOf("grapefruit", "Apple", "Orange", "AAA", "Äardvark", "Banana")));
    }


    @Test
    public void handlesHyphensQuotesAndWhitespace() {
        assertEquals (Helpers.listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit",
                              "\"Hyphenated-words and double quotes\"",
                              "   inappropriate leading space",
                              "Orange"),
                              Helpers.sort(iCUCollatorNormalizer,
                            		  Helpers.listOf("Orange",
                            				  "\"Hyphenated-words and double quotes\"",
                            				  "Banana", "grapefruit",
                            				  "   inappropriate leading space",
                            				  "Äardvark", "Apple", "AAA")));
    }


    @Test
    public void ignoresPunctuationMixedWithSpaces() {
        assertArrayEquals(iCUCollatorNormalizer.normalize("wharton, edith"), 
        		iCUCollatorNormalizer.normalize("wharton edith"));
        assertArrayEquals(iCUCollatorNormalizer.normalize("st. john"), 
        		iCUCollatorNormalizer.normalize("st john"));
    }

}
