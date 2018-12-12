package org.vufind.solr.handler;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

class Log
{
    private static Logger log()
    {
        // Caller's class
        return Logger.getLogger
               (new Throwable().getStackTrace()[2].getClassName());
    }


    public static String formatBytes(byte[] bytes)
    {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append("0x");
            result.append(Integer.toHexString(bytes[i]));
        }

        return result.toString();
    }


    public static String formatBytes(String s)
    {
        try {
            return formatBytes(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void info(String s)
    {
        log().info(s);
    }

    public static void info(String fmt, String s)
    {
        log().info(String.format(fmt, s));
    }
}