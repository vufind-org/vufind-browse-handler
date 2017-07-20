package org.vufind.solr.browse.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.vufind.util.NACONormalizer;

public class NACONormalizerTest
{
    private NACONormalizer nacoNormalizer;

    @Before
    public void setUp()
    {
        nacoNormalizer = new NACONormalizer();
    }


    /*
     * Begin with ICUCollatorNormalizer tests...
     */

    @Test
    public void sortsSimpleStrings()
    {
        assertEquals(listOf("apple", "banana", "cherry", "orange"),
                     sort(listOf("banana", "orange", "apple", "cherry")));
    }


    @Test
    public void sortsDiacriticStrings()
    {
        assertEquals(listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit", "Orange"),
                     sort(listOf("grapefruit", "Apple", "Orange", "AAA", "Äardvark", "Banana")));
    }


    @Test
    public void handlesHyphensQuotesAndWhitespace()
    {
        assertEquals(listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit",
                            "\"Hyphenated-words and double quotes\"",
                            "   inappropriate leading space",
                            "Orange"),
                     sort(listOf("Orange",
                                 "\"Hyphenated-words and double quotes\"",
                                 "Banana", "grapefruit",
                                 "   inappropriate leading space",
                                 "Äardvark", "Apple", "AAA")));
    }

    @Test
    public void ignoresPunctuationMixedWithSpaces()
    {
        assertArrayEquals(nacoNormalizer.normalize("wharton, edith"), nacoNormalizer.normalize("wharton edith"));
        assertArrayEquals(nacoNormalizer.normalize("st. john"), nacoNormalizer.normalize("st john"));
    }

    /*
     * Begin NACO-specific tests
     * See http://www.loc.gov/aba/pcc/naco/normrule-2.html
     */

    @Test
    public void generalCharacters()
    {
        // Delete leading blanks
        assertArrayEquals(nacoNormalizer.normalize("  The Hobbit"), nacoNormalizer.normalize("The Hobbit"));
        // Delete trailing blanks
        assertArrayEquals(nacoNormalizer.normalize("The Hobbit  "), nacoNormalizer.normalize("The Hobbit"));
        // Delete multiple blanks
        assertArrayEquals(nacoNormalizer.normalize("The   Hobbit"), nacoNormalizer.normalize("The Hobbit"));
        // Lowercase equivalent to upper case
        assertArrayEquals(nacoNormalizer.normalize("the hobbit"), nacoNormalizer.normalize("THE HOBBIT"));
    }

    @Test
    public void modifyingDiacritics()
    {
        // *Do not confuse with spacing character equivalents (see Other Special Characters below)
        // Acute
        assertArrayEquals(nacoNormalizer.normalize("écru"), nacoNormalizer.normalize("ecru"));
        // Breve
        assertArrayEquals(nacoNormalizer.normalize("gărahima"), nacoNormalizer.normalize("garahima"));
        // Candrabindu
        assertArrayEquals(nacoNormalizer.normalize("Gam̐va"), nacoNormalizer.normalize("Gamva"));
        // Cedilla
        assertArrayEquals(nacoNormalizer.normalize("façade"), nacoNormalizer.normalize("facade"));
        // Cedilla: COMBINING CEDILLA <U+0327>
        assertArrayEquals(nacoNormalizer.normalize("fa\u0327cade"), nacoNormalizer.normalize("facade"));
        // Cedilla: LATIN SMALL LETTER C WITH CEDILLA <U+00E7>
        assertArrayEquals(nacoNormalizer.normalize("fa\u00E7ade"), nacoNormalizer.normalize("facade"));
        // Circle above, angstrom
        assertArrayEquals(nacoNormalizer.normalize("angstrom"), nacoNormalizer.normalize("angstrom"));           // Circle below
        // Circumflex*
        // Dot below
        assertArrayEquals(nacoNormalizer.normalize("arahaṃ"), nacoNormalizer.normalize("araham"));
        assertArrayEquals(nacoNormalizer.normalize("Tipiṭaka"), nacoNormalizer.normalize("Tipitaka"));
        // Double acute
        // Double tilde (first/second half)
        // Double underscore
        // Grave*
        // Hacek
        // High comma centered
        // High comma off center
        // Left hook
        // Ligature (single) [current preferred representation]
        assertArrayEquals(nacoNormalizer.normalize("Novai͡a"), nacoNormalizer.normalize("Novaia"));
        // Ligature (first/second half) [older convention]
        assertArrayEquals(nacoNormalizer.normalize("vospominanii︠a︡"), nacoNormalizer.normalize("vospominaniia"));
        // Macron
        assertArrayEquals(nacoNormalizer.normalize("Pāli"), nacoNormalizer.normalize("Pali"));
        // Pseudo question mark
        // Right cedilla
        // Right hook, ogonek
        // Superior dot
        assertArrayEquals(nacoNormalizer.normalize("piṅkama"), nacoNormalizer.normalize("pinkama"));
        // Tilde*
        assertArrayEquals(nacoNormalizer.normalize("mañana"), nacoNormalizer.normalize("manana"));
        // Umlaut, diaeresis
        assertArrayEquals(nacoNormalizer.normalize("Ümlaut"), nacoNormalizer.normalize("Umlaut"));
        assertArrayEquals(nacoNormalizer.normalize("ümlaut"), nacoNormalizer.normalize("umlaut"));
        assertArrayEquals(nacoNormalizer.normalize("naïve"), nacoNormalizer.normalize("naive"));
        assertArrayEquals(nacoNormalizer.normalize("noël"), nacoNormalizer.normalize("noel"));
        // Underscore*
        assertArrayEquals(nacoNormalizer.normalize("Kan̲akacapai"), nacoNormalizer.normalize("Kanakacapai"));
        // Upadhmaniya
    }

    @Test
    public void translatedCharacters()
    {
        // Superscript numbers     Numbers Convert to non-superscript equivalent
        assertArrayEquals(nacoNormalizer.normalize("zero⁰"), nacoNormalizer.normalize("zero0"));
        assertArrayEquals(nacoNormalizer.normalize("one¹"), nacoNormalizer.normalize("one1"));
        assertArrayEquals(nacoNormalizer.normalize("two²"), nacoNormalizer.normalize("two2"));
        assertArrayEquals(nacoNormalizer.normalize("three³"), nacoNormalizer.normalize("three3"));
        assertArrayEquals(nacoNormalizer.normalize("four⁴"), nacoNormalizer.normalize("four4"));
        assertArrayEquals(nacoNormalizer.normalize("five⁵"), nacoNormalizer.normalize("five5"));
        assertArrayEquals(nacoNormalizer.normalize("six⁶"), nacoNormalizer.normalize("six6"));
        assertArrayEquals(nacoNormalizer.normalize("seven⁷"), nacoNormalizer.normalize("seven7"));
        assertArrayEquals(nacoNormalizer.normalize("eight⁸"), nacoNormalizer.normalize("eight8"));
        assertArrayEquals(nacoNormalizer.normalize("nine⁹"), nacoNormalizer.normalize("nine9"));
        // Subscript numbers       Numbers Convert to non-subscript equivalent
        assertArrayEquals(nacoNormalizer.normalize("zero₀"), nacoNormalizer.normalize("zero0"));
        assertArrayEquals(nacoNormalizer.normalize("one₁"), nacoNormalizer.normalize("one1"));
        assertArrayEquals(nacoNormalizer.normalize("two₂"), nacoNormalizer.normalize("two2"));
        assertArrayEquals(nacoNormalizer.normalize("three₃"), nacoNormalizer.normalize("three3"));
        assertArrayEquals(nacoNormalizer.normalize("four₄"), nacoNormalizer.normalize("four4"));
        assertArrayEquals(nacoNormalizer.normalize("five₅"), nacoNormalizer.normalize("five5"));
        assertArrayEquals(nacoNormalizer.normalize("six₆"), nacoNormalizer.normalize("six6"));
        assertArrayEquals(nacoNormalizer.normalize("seven₇"), nacoNormalizer.normalize("seven7"));
        assertArrayEquals(nacoNormalizer.normalize("eight₈"), nacoNormalizer.normalize("eight8"));
        assertArrayEquals(nacoNormalizer.normalize("nine₉"), nacoNormalizer.normalize("nine9"));
        // Diagraph AE     AE      Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Ægyptiorum"), nacoNormalizer.normalize("AEgyptiorum"));
        assertArrayEquals(nacoNormalizer.normalize("dæmone"), nacoNormalizer.normalize("daemone"));
        // Diagraph OE     OE      Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Œuvre"), nacoNormalizer.normalize("OEuvre"));
        assertArrayEquals(nacoNormalizer.normalize("œuvre"), nacoNormalizer.normalize("oeuvre"));
        // D with crossbar D       Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Đ U+0110 LATIN CAPITAL LETTER D WITH STROKE"),
                          nacoNormalizer.normalize("Đ U+0110 LATIN CAPITAL LETTER D WITH STROKE"));
        assertArrayEquals(nacoNormalizer.normalize("đ U+0111 LATIN SMALL LETTER D WITH STROKE"),
                          nacoNormalizer.normalize("d U+0111 LATIN SMALL LETTER D WITH STROKE"));
        assertArrayEquals(nacoNormalizer.normalize("aliud đ notatur"), nacoNormalizer.normalize("aliud d notatur"));
        // Eth     D       Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Ðe"), nacoNormalizer.normalize("De"));
        assertArrayEquals(nacoNormalizer.normalize("ðe"), nacoNormalizer.normalize("de"));
        // Lowercase Turkish i     I
        //assertArrayEquals (nacoNormalizer.normalize ("mecmuası"), nacoNormalizer.normalize ("mecmuasi"));
        // Polish L        L       Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Społkanie"), nacoNormalizer.normalize("Spolkanie"));
        assertArrayEquals(nacoNormalizer.normalize("Łuck"), nacoNormalizer.normalize("Luck"));
        assertArrayEquals(nacoNormalizer.normalize("Łuck na Wołyniu"), nacoNormalizer.normalize("Luck na Wolyniu"));
        // Script small L  L
        assertArrayEquals(nacoNormalizer.normalize("ℓ"), nacoNormalizer.normalize("l"));
        assertArrayEquals(nacoNormalizer.normalize("\u2113"), nacoNormalizer.normalize("l"));
        // O Hook  O       Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Ơ"), nacoNormalizer.normalize("O"));
        assertArrayEquals(nacoNormalizer.normalize("\u01A0"), nacoNormalizer.normalize("O"));
        assertArrayEquals(nacoNormalizer.normalize("ơ"), nacoNormalizer.normalize("o"));
        assertArrayEquals(nacoNormalizer.normalize("\u01A1"), nacoNormalizer.normalize("o"));
        // U Hook  U       Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Ư"), nacoNormalizer.normalize("U"));
        assertArrayEquals(nacoNormalizer.normalize("\u01AF"), nacoNormalizer.normalize("U"));
        assertArrayEquals(nacoNormalizer.normalize("ư"), nacoNormalizer.normalize("u"));
        assertArrayEquals(nacoNormalizer.normalize("\u01B0"), nacoNormalizer.normalize("u"));
        // Scandinavian O  O       Upper and lower case
        assertArrayEquals(nacoNormalizer.normalize("Ø"), nacoNormalizer.normalize("O"));
        assertArrayEquals(nacoNormalizer.normalize("ø"), nacoNormalizer.normalize("o"));
        // Icelandic Thorn TH      Upper and lower case
        //assertArrayEquals (nacoNormalizer.normalize ("Þorgils"), nacoNormalizer.normalize ("THorgils"));
        //assertArrayEquals (nacoNormalizer.normalize ("þor"), nacoNormalizer.normalize ("thor"));
        // Eszett symbol   SS      Do not use in NACO records
        assertArrayEquals(nacoNormalizer.normalize("ß"), nacoNormalizer.normalize("SS"));
        assertArrayEquals(nacoNormalizer.normalize("\u00DF"), nacoNormalizer.normalize("SS"));
        // Greek alpha     Uppercase Greek alpha        (U + 0391)      Do not use in NACO 1XX fields
        assertArrayEquals(nacoNormalizer.normalize("α"), nacoNormalizer.normalize("\u0391"));
        // Greek beta      Uppercase Greek beta (U + 0392)      Do not use in NACO 1XX fields
        assertArrayEquals(nacoNormalizer.normalize("β"), nacoNormalizer.normalize("\u0392"));
        // Greek gamma     Uppercase Greek gamma        (U + 0393)      Do not use in NACO 1XX fields
        assertArrayEquals(nacoNormalizer.normalize("γ"), nacoNormalizer.normalize("\u0393"));
    }

    @Test
    public void punctuation()
    {
        // Exclamation mark        Blank
        assertArrayEquals(nacoNormalizer.normalize("Exclaimation!mark"), nacoNormalizer.normalize("Exclaimation mark"));
        assertArrayEquals(nacoNormalizer.normalize("Exclaimation mark at end!"), nacoNormalizer.normalize("Exclaimation mark at end"));
        // Quotation mark  Blank
        assertArrayEquals(nacoNormalizer.normalize("Quotation\"mark"), nacoNormalizer.normalize("Quotation mark"));
        // Apostrophe      Delete
        assertArrayEquals(nacoNormalizer.normalize("it's"), nacoNormalizer.normalize("its"));
        // Opening parenthesis     Blank
        assertArrayEquals(nacoNormalizer.normalize("Opening(parenthesis"), nacoNormalizer.normalize("Opening parenthesis"));
        // Closing parenthesis     Blank
        assertArrayEquals(nacoNormalizer.normalize("Closing)parenthesis"), nacoNormalizer.normalize("Closing parenthesis"));
        // Hyphen, minus-  Blank
        assertArrayEquals(nacoNormalizer.normalize("inter-alia"), nacoNormalizer.normalize("inter alia"));
        // Opening square bracket  Delete
        assertArrayEquals(nacoNormalizer.normalize("Opening[square bracket"), nacoNormalizer.normalize("Openingsquare bracket"));
        // Closing square bracket  Delete
        assertArrayEquals(nacoNormalizer.normalize("Closing]square bracket"), nacoNormalizer.normalize("Closingsquare bracket"));
        // Opening curly bracket   Blank
        assertArrayEquals(nacoNormalizer.normalize("Opening{curly bracket"), nacoNormalizer.normalize("Opening curly bracket"));
        // Closing curly bracket   Blank
        assertArrayEquals(nacoNormalizer.normalize("Closing}curly bracket"), nacoNormalizer.normalize("Closing curly bracket"));
        // Less-than sign  Blank
        assertArrayEquals(nacoNormalizer.normalize("one<three"), nacoNormalizer.normalize("one three"));
        // Greater-than sign       Blank
        assertArrayEquals(nacoNormalizer.normalize("three>one"), nacoNormalizer.normalize("three one"));
        // Semicolon       Blank
        assertArrayEquals(nacoNormalizer.normalize("semicolon;semicolon"), nacoNormalizer.normalize("semicolon semicolon"));
        // Colon:  Blank
        assertArrayEquals(nacoNormalizer.normalize("colon:colon"), nacoNormalizer.normalize("colon:colon"));
        // Period, decimal point   Blank
        assertArrayEquals(nacoNormalizer.normalize("19.95"), nacoNormalizer.normalize("19 95"));
        assertArrayEquals(nacoNormalizer.normalize("first.second"), nacoNormalizer.normalize("first second"));
        // Question mark   Blank
        assertArrayEquals(nacoNormalizer.normalize("question?mark"), nacoNormalizer.normalize("question mark"));
        // Inverted question mark  Blank
        assertArrayEquals(nacoNormalizer.normalize("Inverted¿question¿mark"),
                          nacoNormalizer.normalize("Inverted question mark"));
        // Inverted exclamation mark       Blank
        assertArrayEquals(nacoNormalizer.normalize("Inverted¡exclamation\u00A1mark"),
                          nacoNormalizer.normalize("Inverted exclamation mark"));
        // comma   Comma or blank  The first comma in $a is retained; all other converted to blank
        // NOTE: first comma is too fussy right now, all commas will go to blank.
        assertArrayEquals(nacoNormalizer.normalize("comma,comma,comma"), nacoNormalizer.normalize("comma comma comma"));
    }

    // non-NACO extras
    // Treat left & right single quotes same as ASCII single quote: delete
    @Test
    public void punctuationLeftRightSingleQuotes()
    {
        // LEFT SINGLE QUOTATION MARK      Delete
        assertArrayEquals(nacoNormalizer.normalize("left‘quote"), nacoNormalizer.normalize("leftquote"));
        // RIGHT SINGLE QUOTATION MARK     Delete
        assertArrayEquals(nacoNormalizer.normalize("l’enfant"), nacoNormalizer.normalize("lenfant"));
    }

    @Test
    public void punctuationLeftRightDoubleQuotes()
    {
        // LEFT DOUBLE QUOTATION MARK      Blank
        assertArrayEquals(nacoNormalizer.normalize("left“quote"), nacoNormalizer.normalize("left quote"));
        // RIGHT DOUBLE QUOTATION MARK     Blank
        assertArrayEquals(nacoNormalizer.normalize("right”quote"), nacoNormalizer.normalize("right”quote"));
        // LEFT-POINTING DOUBLE ANGLE QUOTATION MARK      Blank
        assertArrayEquals(nacoNormalizer.normalize("left«quote"), nacoNormalizer.normalize("left quote"));
        // RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK     Blank
        assertArrayEquals(nacoNormalizer.normalize("right»quote"), nacoNormalizer.normalize("right”quote"));
    }

    @Test
    public void otherSpecialCharacters()
    {
        // Character       Character       Comments
        // Music flat sign Retain
        // Number sign     Retain
        // Slash   Blank
        assertArrayEquals(nacoNormalizer.normalize("slash/slash"), nacoNormalizer.normalize("slash slash"));
        // Reverse slash   Blank
        assertArrayEquals(nacoNormalizer.normalize("reverse\\slash"), nacoNormalizer.normalize("reverse slash"));
        // Commercial at sign      Retain
        // Ampersand       Retain
        // Asterisk        Blank
        assertArrayEquals(nacoNormalizer.normalize("asterisk*asterisk"), nacoNormalizer.normalize("asterisk asterisk"));
        // Vertical bar (fill)     Blank
        assertArrayEquals(nacoNormalizer.normalize("Vertical|bar"), nacoNormalizer.normalize("Vertical bar"));
        // Percent Blank
        assertArrayEquals(nacoNormalizer.normalize("percent%percent"), nacoNormalizer.normalize("percent percent"));
        // Equals sign     Blank
        assertArrayEquals(nacoNormalizer.normalize("equals=equals"), nacoNormalizer.normalize("equals equals"));
        // Plus sign       Retain
        // Plus or minus U+00B1   Blank
        assertArrayEquals(nacoNormalizer.normalize("plus\u00B1or±minus"), nacoNormalizer.normalize("plus or minus"));
        // Superscript plus, minus Blank
        assertArrayEquals(nacoNormalizer.normalize("superscript\u207Aplus"), nacoNormalizer.normalize("superscript plus"));
        assertArrayEquals(nacoNormalizer.normalize("superscript\u207Bminus"), nacoNormalizer.normalize("superscript minus"));
        // Patent mark     Blank
        assertArrayEquals(nacoNormalizer.normalize("patent®mark"), nacoNormalizer.normalize("patent mark"));
        // Sound recording copyright       Blank
        assertArrayEquals(nacoNormalizer.normalize("sound recording℗copyright"), nacoNormalizer.normalize("sound recording copyright"));
        // Copyright sign  Blank
        assertArrayEquals(nacoNormalizer.normalize("copyright©sign"), nacoNormalizer.normalize("copyright sign"));
        // Dollar sign     Retain
        // British pound   Retain
        // Degree sign U+00B0     Blank
        assertArrayEquals(nacoNormalizer.normalize("degree°sign"), nacoNormalizer.normalize("degree sign"));
        // Spacing circumflex      Blank
        assertArrayEquals(nacoNormalizer.normalize("spacing^circumflex"), nacoNormalizer.normalize("spacing circumflex"));
        // Spacing underscore      Blank
        assertArrayEquals(nacoNormalizer.normalize("spacing_underscore"), nacoNormalizer.normalize("spacing underscore"));
        // Spacing grave   Blank
        assertArrayEquals(nacoNormalizer.normalize("spacing`grave"), nacoNormalizer.normalize("spacing grave"));
        // Spacing tilde   Blank
        assertArrayEquals(nacoNormalizer.normalize("spacing~tilde"), nacoNormalizer.normalize("spacing tilde"));
        // Euro sign       Retain  Do not use in NACO records
        // Music sharp sign        Retain
        // Alif U+02BC    Delete
        assertArrayEquals(nacoNormalizer.normalize("alif\u02BCalif"), nacoNormalizer.normalize("alifalif"));
        // Ayn U+02BB     Delete
        assertArrayEquals(nacoNormalizer.normalize("ayn\u02BBayn"), nacoNormalizer.normalize("aynayn"));
        // Hard sign U+02BA       Delete
        assertArrayEquals(nacoNormalizer.normalize("hard\u02BAsign"), nacoNormalizer.normalize("hardsign"));
        // Soft sign U+02B9      Delete
        assertArrayEquals(nacoNormalizer.normalize("soft\u02B9sign"), nacoNormalizer.normalize("softsign"));
        // Middle dot U+00B7      Blank
        assertArrayEquals(nacoNormalizer.normalize("middle\u00B7dot"), nacoNormalizer.normalize("middle dot"));
    }

    @Test
    public void obsoleteConversions()
    {
        // MODIFIER LETTER LEFT HALF RING <U02BF>       Delete -- formerly used for Ayn
        assertArrayEquals(nacoNormalizer.normalize("Mua\u02BFllim"), nacoNormalizer.normalize("Muallim"));
    }

    // Some NACO-specific sorting

    @Test
    public void sortAuthorNames()
    {
        assertEquals(listOf("ʿAbd al-ʿAzīz, Kamāl",
                            "'Abd al-Bari, 'Abd al-Majid al-Shaykh",
                            "Abd al-Fattāḥ, Khālid Salīm",
                            "ʿAbd al-Mawjūd, ʿĀdil Aḥmad",
                            "ʿAbd al-Qaddūs, Iḥsān",
                            "ʿAdī, Saʿīd"),
                     sort(listOf("ʿAbd al-Qaddūs, Iḥsān",
                                 "'Abd al-Bari, 'Abd al-Majid al-Shaykh",
                                 "ʿAdī, Saʿīd",
                                 "ʿAbd al-Mawjūd, ʿĀdil Aḥmad",
                                 "ʿAbd al-ʿAzīz, Kamāl",
                                 "Abd al-Fattāḥ, Khālid Salīm")));
    }

    @Test
    public void normalizeRussianTitles()
    {
        assertArrayEquals(nacoNormalizer.normalize("Mif v folʹklornykh tradit͡sii͡akh i kulʹture noveĭshego vremeni"),
                          nacoNormalizer.normalize("Mif v fol'klornykh tradit͡sii͡akh i kul'ture noveĭshego vremeni"));
        assertArrayEquals(nacoNormalizer.normalize("Mif v folʹklornykh tradit͡sii͡akh i kulʹture noveĭshego vremeni"),
                          nacoNormalizer.normalize("Mif v folklornykh traditsiiakh i kulture noveĭshego vremeni"));
    }

    @Test
    public void singleQuoteLeftRightSingleQuoteEquivalence()
    {
        assertArrayEquals(nacoNormalizer.normalize("L'enfant"),
                          nacoNormalizer.normalize("L’enfant"));
        assertArrayEquals(nacoNormalizer.normalize("left‘quote"), nacoNormalizer.normalize("left'quote"));
        assertArrayEquals(nacoNormalizer.normalize("l’enfant"), nacoNormalizer.normalize("l'enfant"));
    }
    //
    // Helpers
    //

    private List<String> listOf(String ... args)
    {
        List<String> result = new ArrayList<String> ();
        for (String s : args) {
            result.add(s);
        }

        return result;
    }


    // http://stackoverflow.com/questions/5108091/java-comparator-for-byte-array-lexicographic
    private int compareByteArrays(byte[] left, byte[] right)
    {
        for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
            int a = (left[i] & 0xff);
            int b = (right[j] & 0xff);
            if (a != b) {
                return a - b;
            }
        }
        return left.length - right.length;
    }


    private List<String> sort(List<String> list)
    {
        List<String> result = new ArrayList<String> ();
        result.addAll(list);

        Collections.sort(result, new Comparator<String> () {
            public int compare(String s1, String s2) {
                return compareByteArrays(nacoNormalizer.normalize(s1),
                                         nacoNormalizer.normalize(s2));
            }
        });

        return result;
    }

}
