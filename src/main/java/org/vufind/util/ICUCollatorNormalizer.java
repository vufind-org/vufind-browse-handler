package org.vufind.util;

import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import java.util.regex.Pattern;

/**
 * Normalizer class which uses the ICU <code>Collator<code> class to produce collation byte arrays.
 * The use of <code>Collator<code> takes into account diacritics and other Unicode features.
 * This normalizer should be suitable for most text fields.
 *
 * @author Mark Triggs <mark@teaspoon-consulting.com>
 * @author Tod Olson <tod@uchicago.edu>
 *
 */

public class ICUCollatorNormalizer implements Normalizer
{
    protected Collator collator;

    protected Pattern junkregexp =
        Pattern.compile("\\([^a-z0-9\\p{L} ]\\)");


    public ICUCollatorNormalizer()
    {
        collator = Collator.getInstance();
        // Ignore case for the purposes of comparisons.
        collator.setStrength(Collator.SECONDARY);
    }

    // TODO: remove getInstance when no longer needed
    public static ICUCollatorNormalizer getInstance() throws Exception
    {
        ICUCollatorNormalizer iCUCollatorNormalizer;

        if (Utils.getEnvironment("NORMALISER") != null) {
            String normaliserClass = Utils.getEnvironment("NORMALISER");

            iCUCollatorNormalizer = (ICUCollatorNormalizer)(Class.forName(normaliserClass)
                                    .getConstructor()
                                    .newInstance());
        } else {
            iCUCollatorNormalizer = new ICUCollatorNormalizer();
        }

        return iCUCollatorNormalizer;
    }

    // Breaking out the CollationKey makes testing and debugging easier
    public CollationKey normalizeToKey(String s)
    {
        s = s.replaceAll("-", "")
            .replaceAll("\\p{Punct}", " ")
            .replaceAll(" +", " ")
            .trim();

        s = junkregexp.matcher(s) .replaceAll("");

        return collator.getCollationKey(s);
    }

    public byte[] normalize(String s)
    {
        return normalizeToKey(s).toByteArray();
    }
}
