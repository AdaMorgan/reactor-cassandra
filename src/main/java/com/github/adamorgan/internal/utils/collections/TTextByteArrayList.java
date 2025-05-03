package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.internal.utils.requestbody.BinaryType;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class TTextByteArrayList extends TByteArrayList<String>
{
    public TTextByteArrayList(int initialCapacity, Collection<ByteBuf> values)
    {
        super(values, BinaryType.LONG_STRING::decode);
    }
}
