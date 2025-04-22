package com.datastax.api.utils.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Map;

public class Pair<T>
{
    private final String key;
    private final T value;

    public Pair(Map.Entry<String, T> entry)
    {
        this.key = entry.getKey();
        this.value = entry.getValue();
    }

    public ByteBuf asByteBuf()
    {
        return Unpooled.buffer();
    }
}
