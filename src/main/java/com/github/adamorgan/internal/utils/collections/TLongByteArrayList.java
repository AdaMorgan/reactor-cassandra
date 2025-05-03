package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.internal.utils.requestbody.BinaryType;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class TLongByteArrayList extends TByteArrayList<Long>
{
    public TLongByteArrayList(int initialCapacity, Collection<ByteBuf> values)
    {
        super(values, BinaryType.BIGINT::decode);
    }
}
