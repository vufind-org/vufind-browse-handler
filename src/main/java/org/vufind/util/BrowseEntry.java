package org.vufind.util;

public class BrowseEntry
{
    public byte[] key;
    public String key_text;
    public String value;

    public BrowseEntry(byte[] key, String key_text, String value)
    {
        this.key = key;
        this.key_text = key_text;
        this.value = value;
    }
}
