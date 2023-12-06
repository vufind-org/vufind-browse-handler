package org.vufind.util;

import org.marc4j.callnum.LCCallNumber;
import java.util.logging.Logger;

public class LCCallNormalizer implements Normalizer
{

    @Override
    public byte[] normalize(String s)
    {
        LCCallNumber lccn = new LCCallNumber(s);
        String n = lccn.getShelfKey();
        // for debugging; if needed
        //log.info("Normalized: " + s + " to: " + n);
        byte[] key = (n == null) ? null : n.getBytes();
        return key;
    }

    private static final Logger log = Logger.getLogger(LCCallNormalizer.class.getName());
}
