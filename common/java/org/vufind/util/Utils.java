package org.vufind.util;

public class Utils
{
    public static String getEnvironment(String var)
    {
        return (System.getenv(var) != null) ?
               System.getenv(var) : System.getProperty(var.toLowerCase());
    }
}
