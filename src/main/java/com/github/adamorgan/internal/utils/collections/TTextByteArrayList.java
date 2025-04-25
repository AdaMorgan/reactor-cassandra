package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.api.utils.data.DataType;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class TTextByteArrayList extends TByteArrayList<String>
{
    public TTextByteArrayList(int initialCapacity, Collection<ByteBuf> values)
    {
        super(initialCapacity, values, DataType.LONG_STRING::decode);
    }
}
