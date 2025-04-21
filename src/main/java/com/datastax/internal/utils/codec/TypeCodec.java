package com.datastax.internal.utils.codec;

import io.netty.buffer.ByteBuf;

public interface TypeCodec<T>
{
    ByteBuf encode(ByteBuf buffer, T value);

    T decode(ByteBuf buffer);
}
