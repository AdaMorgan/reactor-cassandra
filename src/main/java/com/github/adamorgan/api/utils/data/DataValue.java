package com.github.adamorgan.api.utils.data;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class DataValue<T>
{
    protected final T value;

    public DataValue(T value)
    {
        this.value = value;
    }

    @Nonnull
    public ByteBuf asByteBuf()
    {
        return null;
    }
}
