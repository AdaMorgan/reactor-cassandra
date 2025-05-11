package com.github.adamorgan.internal.utils.compress;

import com.github.adamorgan.api.utils.Compression;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class NoopCompressor<T extends ByteBuf> implements Compressor<T>
{
    @Nonnull
    @Override
    public Compression getType()
    {
        return Compression.NONE;
    }

    @Nonnull
    @Override
    public T pack(T body)
    {
        return body;
    }

    @Nonnull
    @Override
    public T unpack(T body)
    {
        return body;
    }

    @Nonnull
    @Override
    public T packWithoutLength(T body)
    {
        return body;
    }

    @Nonnull
    @Override
    public T unpackWithoutLength(T body, int length)
    {
        return body;
    }
}
