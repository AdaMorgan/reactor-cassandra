package com.github.adamorgan.api.utils;

import com.github.adamorgan.internal.utils.compress.Compressor;
import com.github.adamorgan.internal.utils.compress.Lz4Compressor;
import com.github.adamorgan.internal.utils.compress.NoopCompressor;
import com.github.adamorgan.internal.utils.compress.SnappyCompressor;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

/**
 * Compression algorithms that can be used with CQL Binary Protocol.
 *
 * @see com.github.adamorgan.api.LibraryBuilder#setCompression(Compression)
 */
public enum Compression
{
    NONE("", new NoopCompressor<>()),
    LZ4("lz4", new Lz4Compressor()),
    SNAPPY("snappy", new SnappyCompressor());

    private final String key;
    private final Compressor<ByteBuf> compressor;

    Compression(String key, Compressor<ByteBuf> compressor)
    {
        this.key = key;
        this.compressor = compressor;
    }

    @Override
    public String toString()
    {
        return key;
    }

    @Nonnull
    public ByteBuf pack(ByteBuf body)
    {
        return compressor.pack(body);
    }

    @Nonnull
    public ByteBuf unpack(ByteBuf body)
    {
        return compressor.unpack(body);
    }
}
