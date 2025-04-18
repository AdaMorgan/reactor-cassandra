package com.datastax.api.utils.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;

public final class DataValue<T> implements ByteBufConvertible
{
    private final T value;

    public DataValue(T value)
    {
        this.value = value;
    }

    @Override
    public ByteBuf asByteBuf()
    {
        return null;
    }
}
