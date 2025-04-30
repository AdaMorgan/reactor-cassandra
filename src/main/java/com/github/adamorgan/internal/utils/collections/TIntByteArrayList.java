package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.api.utils.data.DataType;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class TIntByteArrayList extends TByteArrayList<Integer>
{
    public TIntByteArrayList(int initialCapacity, Collection<ByteBuf> values)
    {
        super(values, DataType.INT::decode);
    }
}
