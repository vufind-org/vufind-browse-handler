package org.vufind.solr.browse.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.vufind.util.TopicNormalizer;

public class TopicNormalizerTest
{
    private TopicNormalizer normalizer;

    @Before
    public void setUp() {
        normalizer = new TopicNormalizer ();
    }


    @Test
    public void sortsSimpleStrings() {
        assertEquals(Helpers.listOf("apple", "banana", "cherry", "orange"),
                Helpers.sort(normalizer,
                        Helpers.listOf ("banana", "orange", "apple", "cherry")));
    }


    @Test
    public void sortsDiacriticStrings() {
        assertEquals(Helpers.listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit", "Orange"),
                Helpers.sort(normalizer,
                        Helpers.listOf("grapefruit", "Apple", "Orange", "AAA", "Äardvark", "Banana")));
    }


    @Test
    public void handlesHyphensQuotesAndWhitespace() {
        assertEquals (Helpers.listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit",
                              "\"Hyphenated-words and double quotes\"",
                              "   inappropriate leading space",
                              "Orange"),
                              Helpers.sort(normalizer,
                                      Helpers.listOf("Orange",
                                              "\"Hyphenated-words and double quotes\"",
                                              "Banana", "grapefruit",
                                              "   inappropriate leading space",
                                              "Äardvark", "Apple", "AAA")));
    }


    @Test
    public void ignoresPunctuationMixedWithSpaces() {
        assertArrayEquals(normalizer.normalize("wharton, edith"),
                normalizer.normalize("wharton edith"));
        assertArrayEquals(normalizer.normalize("st. john"),
                normalizer.normalize("st john"));
    }


    @Test
    public void ignoresDoubleHyphens() {
        assertEquals(Helpers.listOf(
                "University of Chicago--Accreditation.",
                "University of Chicago -- Admission.",
                "University of Chicago. Alumni Association"),
                Helpers.sort(normalizer,
                        Helpers.listOf(
                                "University of Chicago. Alumni Association",
                                "University of Chicago--Accreditation.",
                                "University of Chicago -- Admission.")));
    }



}
