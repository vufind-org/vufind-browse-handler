package org.vufind.util;

import java.util.Arrays;
import java.util.Locale;

public class Utils
{
    public static String getEnvironment(String var)
    {
        return (System.getenv(var) != null) ?
            System.getenv(var) : System.getProperty(var.toLowerCase(Locale.ROOT));
    }

    public static void printDeprecationWarning(String ... lines) {
        int maxLineLength = Arrays.stream(lines).map(String::length).max(Integer::compare).orElse(70);

        String separator = new String(new char[maxLineLength]).replace('\0', '*');

        System.err.print("\n\n\n");
        System.err.println(separator);
        System.err.println("DEPRECATION WARNING:\n");
        for (String line : lines) {
            System.err.println(line);
        }
        System.err.println(separator);
        System.err.print("\n\n\n");
    }
}
