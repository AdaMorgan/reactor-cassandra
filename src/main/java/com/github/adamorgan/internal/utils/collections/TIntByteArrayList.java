package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.internal.utils.requestbody.BinaryType;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class TIntByteArrayList extends TByteArrayList<Integer>
{
    public TIntByteArrayList(int initialCapacity, Collection<ByteBuf> values)
    {
        super(values, BinaryType.INT::decode);
    }
}
