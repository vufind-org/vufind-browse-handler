package org.vufind.util;

public class BrowseEntry
{
    public byte[] key;
    public String value;

    public BrowseEntry (byte[] key, String value)
    {
        this.key = key;
        this.value = value;
    }
}
