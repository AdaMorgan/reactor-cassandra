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

    enum Codec
    {
        INT(Integer.class),
        BIGINT(Long.class),
        UNKNOWN(Object.class);

        <R> Codec(Class<R> type)
        {

        }

        public static <R> ByteBuf fromCodec(Class<R> type)
        {
            for (Codec value : values())
            {
                if (value.getClass() == type)
                {
                    return null;
                }
            }

            return null;
        }
    }
}
