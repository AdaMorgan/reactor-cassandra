package com.github.adamorgan.api.utils.data;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public interface TypeCodec<T>
{
    @Nonnull
    ByteBuf encode(ByteBuf buffer, T value);

    @Nonnull
    T decode(ByteBuf buffer);
}
