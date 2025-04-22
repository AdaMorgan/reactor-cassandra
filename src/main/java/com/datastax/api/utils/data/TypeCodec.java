package com.datastax.api.utils.data;

import io.netty.buffer.ByteBuf;

public interface TypeCodec<T>
{
    ByteBuf encode(ByteBuf buffer, T value);

    T decode(ByteBuf buffer);
}
