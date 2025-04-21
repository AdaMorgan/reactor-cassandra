package com.datastax.api.utils.data;

import io.netty.buffer.ByteBuf;

public class DataValue<T>
{
    protected final T value;

    public DataValue(T value)
    {
        this.value = value;
    }

    public ByteBuf asByteBuf()
    {
        return null;
    }
}
