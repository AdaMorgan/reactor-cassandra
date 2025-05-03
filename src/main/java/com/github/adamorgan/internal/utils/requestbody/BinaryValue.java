package com.github.adamorgan.internal.utils.requestbody;

import io.netty.buffer.ByteBuf;

import java.util.Map;

public class BinaryValue implements BinaryCodec
{

    public static <R> void pack0(ByteBuf buffer, R value)
    {
        if (value instanceof Long)
        {
            BinaryType.BIGINT.pack(buffer, (Long) value);
            return;
        }

        if (value instanceof String)
        {
            BinaryType.LONG_STRING.pack(buffer, (String) value);
            return;
        }

        throw new UnsupportedOperationException("Cannot pack value of type " + value.getClass().getName());
    }

    public static <R> void pack0(ByteBuf buffer, Map.Entry<String, ? super R> entry)
    {
        BinaryType.STRING.pack(buffer, entry.getKey());
        pack0(buffer, entry.getValue());
    }
}
