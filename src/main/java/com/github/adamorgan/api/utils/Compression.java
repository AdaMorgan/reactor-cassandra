package com.github.adamorgan.api.utils;

public enum Compression
{
    NONE(""),
    LZ4("lz4"),
    SNAPPY("snappy");

    private final String key;

    Compression(String key)
    {
        this.key = key;
    }

    @Override
    public String toString()
    {
        return key;
    }
}
