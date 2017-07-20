package org.vufind.util;

/**
 * Simple class to instantiate and return a Normalizer object.
 *
 * @author Tod Olson <tod@uchicago.edu>
 *
 */

public class NormalizerFactory
{

    private static String defaultNormalizerClassName = "org.vufind.util.ICUCollatorNormalizer";

    public static String getDefaultNormalizerClassName()
    {
        return defaultNormalizerClassName;
    }

    /**
     * Create an instance of a class which implements the <code>Normalizer</code> interface.
     *
     * If normalizedClass is null, return the default normalizer.
     *
     * @param normalizerClass name of a <code>Normalizer</code> class
     * @return instance of the named <code>Normalizer</code> class
     * @throws Exception if anything goes wrong with creating the class
     */
    public static Normalizer getNormalizer(String normalizerClass) throws Exception
    {
        if (normalizerClass == null) {
            return getNormalizer();
        } else {
            return (Normalizer)(Class.forName(normalizerClass)
                                .getConstructor()
                                .newInstance());
        }
    }

    /**
     * Create an instance of the default <code>Normalizer</code> class.
     *
     * @return instance of the default <code>Normalizer</code> class
     * @throws Exception if anything goes wrong with creating the class
     */
    public static Normalizer getNormalizer() throws Exception
    {
        return getNormalizer(defaultNormalizerClassName);
    }

}
