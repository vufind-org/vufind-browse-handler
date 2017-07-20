package org.vufind.util;

import java.util.regex.Pattern;

import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;

/**
 * Browse normalizer which implements the NACO Normalization Rules.
 *
 * This class diverges from the NACO Normalization Rules:
 *
 * Additional normalization:
 * <ol>
 * <li>
 * single quotes and left and right single and double quotes
 * </li>
 * <li>
 * European-style quotes, «»:<br>
 * LEFT-POINTING DOUBLE ANGLE QUOTATION MARK &lt;U+00AB><br>
 * RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK &lt;U+00BB>
 * </li>
 * <li>
 * MODIFIER LETTER LEFT HALF RING <U02BF>
 * </li>
 * </ol>
 *
 * Not yet implemented:
 * <ol>
 * <li>
 * commas: all commas are converted to blanks, first comma in ‡a is <em>not</em> retained
 * </li>
 * <li>
 * eszedt, and thorn are not normalized to their two-character equivalents
 * </li>
 * <li>
 * </li>
 * </ol>
 *
 *
 * <p>Based on {@link ICUCollatorNormalizer}, by Mark Triggs</p>
 *
 * @author Tod Olson, University of Chicago
 *
 * @see <a href="http://www.loc.gov/aba/pcc/naco/normrule-2.html">Authority File Comparison Rules (NACO Normalization)</a>
 */

public class NACONormalizer implements Normalizer
{
    protected Collator collator;

    /**
     * Characters that will be deleted during normalization.
     */
    static private String deleteChars = "['\\[\\]‘’\u02BA\u02BB\u02BC\u02B9\u02BF]";

    /**
     * Characters that will be converted to spaces during normalization.
     */
    static private String spaceChars = "[\\p{Punct}¿¡“”«»±⁺⁻℗®©°·]";

    /**
     * Pattern to match characters that will be deleted during normalization.
     */
    static private Pattern deletePattern = Pattern.compile(deleteChars);

    /**
     * Pattern to match characters to treat as spaces during normalization.
     */
    static private Pattern spacePattern = Pattern.compile(spaceChars);

    /**
     * Pattern for squashing repeated whitespace to a single character.
     */
    static private Pattern whitespacePattern = Pattern.compile("\\s+");

    public NACONormalizer()
    {
        collator = Collator.getInstance();
        // Ignore case and diacritics for the purposes of comparisons.
        // Use PRIMARY unless we prove we need something different
        collator.setStrength(Collator.PRIMARY);

    }

    // TODO: remove getInstance when no longer needed
    public static NACONormalizer getInstance() throws Exception
    {
        NACONormalizer nacoNormalizer;

        if (Utils.getEnvironment("NORMALISER") != null) {
            String normalizerClass = Utils.getEnvironment("NORMALISER");

            nacoNormalizer = (NACONormalizer)(Class.forName(normalizerClass)
                                              .getConstructor()
                                              .newInstance());
        } else {
            nacoNormalizer = new NACONormalizer();
        }

        return nacoNormalizer;
    }

    /**
     * Computes ICU collation key for the input string.
     *
     * <p>Breaking out the CollationKey makes testing and debugging easier.</p>
     *
     * <p>
     * Implementation note:
     * Using cached patterns rather than {@code String#replaceAll} seems to speed up
     * key production significantly, roughly 20% in {@code main()}.
     * </p>
     *
     * @param string to normalize
     * @return collation key object
     */
    public CollationKey normalizeToKey(String s)
    {
        s = deletePattern.matcher(s).replaceAll("");
        s = spacePattern.matcher(s).replaceAll(" ");
        s = whitespacePattern.matcher(s).replaceAll(" ");
        s = s.trim();
        return collator.getCollationKey(s);
    }

    public byte[] normalize(String s)
    {
        return normalizeToKey(s).toByteArray();
    }

    /**
     * Read lines from stdin and write normalize output to stdout.
     * Useful for benchmarking, debugging.
     */
    public static void main(String[] args)
    {
        try {
            NACONormalizer norm = new NACONormalizer();

            java.io.InputStreamReader in= new java.io.InputStreamReader(System.in);
            java.io.BufferedReader input = new java.io.BufferedReader(in);
            String str;
            while ((str = input.readLine()) != null) {
                System.out.println(norm.normalize(str));
            }
        } catch (java.io.IOException io) {
            io.printStackTrace();
        }
    }
}
