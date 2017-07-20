package org.vufind.util;

import org.solrmarc.callnum.DeweyCallNumber;
import java.util.logging.Logger;

public class DeweyCallNormalizer implements Normalizer
{

    @Override
    public byte[] normalize(String s)
    {
        DeweyCallNumber dewey = new DeweyCallNumber(s);
        String n = dewey.getShelfKey();
        // for debugging; if needed
        //log.info("Normalized: " + s + " to: " + n);
        byte[] key = (n == null) ? null : n.getBytes();
        return key;
    }

    private static final Logger log = Logger.getLogger(DeweyCallNormalizer.class.getName());
}
