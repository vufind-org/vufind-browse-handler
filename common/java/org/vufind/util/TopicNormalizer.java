package org.vufind.util;

//import java.util.regex.*;

import com.ibm.icu.text.Collator;

/**
 * Normalizes topic strings to ignore double-hyphens and punctution,
 * and then uses the ICU <code>Collator<code> class to produce collation byte arrays.
 * The use of <code>Collator<code> takes into account diacritics and other Unicode features.
 *
 * This normalizer allows subjects to sort in alphabetical order regardless of added hyphens,
 * and should help give flexibility when the user typing in a subject does no know how it is
 * subdivided.
 *
 * @author Mark Triggs <mark@dishevelled.net>
 * @author Tod Olson <tod@uchicago.edu>
 *
 */

public class TopicNormalizer implements Normalizer
{
    protected Collator collator;

//    protected Pattern junkregexp =
//        Pattern.compile("\\([^a-z0-9\\p{L} ]\\)");


    public TopicNormalizer()
    {
        collator = Collator.getInstance();
        // Ignore case for the purposes of comparisons.
        collator.setStrength(Collator.SECONDARY);
    }

    public byte[] normalize(String s)
    {
        s = s.replaceAll("--", " ")
            .replaceAll("\\p{Punct}", " ")
            .replaceAll(" +", " ")
            .trim();
        //s = junkregexp.matcher(s).replaceAll("");

        return collator.getCollationKey(s).toByteArray();
    }
}
