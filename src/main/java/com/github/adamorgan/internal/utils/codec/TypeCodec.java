package com.github.adamorgan.internal.utils.codec;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public interface TypeCodec<T>
{
    @Nonnull
    ByteBuf pack(ByteBuf buffer, T value);

    @Nonnull
    T decode(ByteBuf buffer);

}
