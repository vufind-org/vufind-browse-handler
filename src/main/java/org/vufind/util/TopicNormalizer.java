package org.vufind.util;

public class TopicNormalizer implements Normalizer
{
    private Normalizer defaultNormalizer;

    public TopicNormalizer() {
        defaultNormalizer = new ICUCollatorNormalizer();
    }

    // Separator defined by VuFind's marc.properties for topic_browse field.
    final static String TOPIC_TERM_SEPARATOR = "\u2002";

    @Override
    public byte[] normalize(String s)
    {
        // Treat topic term separators as spaces when producing a sort key
        return defaultNormalizer.normalize(s.replace(TOPIC_TERM_SEPARATOR, " "));
    }

    @Override
    public String headingForAuthQuery(String heading) {
        // Auth DB uses single spaces as delimiters while topics use em spaces
        return heading.replace(TOPIC_TERM_SEPARATOR, " ");
    }

}
