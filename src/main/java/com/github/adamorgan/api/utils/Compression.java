package com.github.adamorgan.api.utils;

import com.github.adamorgan.internal.utils.compress.Compressor;
import com.github.adamorgan.internal.utils.compress.Lz4Compressor;
import com.github.adamorgan.internal.utils.compress.NoopCompressor;
import com.github.adamorgan.internal.utils.compress.SnappyCompressor;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;

/**
 * Compression algorithms that can be used with CQL Binary Protocol.
 *
 * @see com.github.adamorgan.api.LibraryBuilder#setCompression(Compression)
 */
public enum Compression
{
    NONE("", new NoopCompressor<>(), Compressor::pack, Compressor::unpack),
    LZ4("lz4", new Lz4Compressor(), Compressor::pack, Compressor::unpack),
    SNAPPY("snappy", new SnappyCompressor(), Compressor::pack, Compressor::unpack);

    private final String key;
    private final Compressor<ByteBuf> compressor;
    private final BiFunction<Compressor<ByteBuf>, ByteBuf, ByteBuf> pack;
    private final BiFunction<Compressor<ByteBuf>, ByteBuf, ByteBuf> unpack;

    Compression(String key, Compressor<ByteBuf> compressor, BiFunction<Compressor<ByteBuf>, ByteBuf, ByteBuf> pack, BiFunction<Compressor<ByteBuf>, ByteBuf, ByteBuf> unpack)
    {
        this.key = key;
        this.compressor = compressor;
        this.pack = pack;
        this.unpack = unpack;
    }

    @Override
    public String toString()
    {
        return key;
    }

    @Nonnull
    public ByteBuf pack(ByteBuf body)
    {
        return pack.apply(compressor, body);
    }

    @Nonnull
    public ByteBuf unpack(ByteBuf body)
    {
        return unpack.apply(compressor, body);
    }
}
