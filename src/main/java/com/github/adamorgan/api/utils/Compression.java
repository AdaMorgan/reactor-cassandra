package com.github.adamorgan.api.utils;

import javax.annotation.Nonnull;

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

    /**
     * The key used for the gateway query to enable this compression
     *
     * @return The query key
     */
    @Nonnull
    public String getKey()
    {
        return key;
    }
}
