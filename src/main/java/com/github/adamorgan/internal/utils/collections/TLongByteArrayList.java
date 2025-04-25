package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.api.utils.data.DataType;
import io.netty.buffer.ByteBuf;

import java.util.Collection;

public class TLongByteArrayList extends TByteArrayList<Long>
{
    public TLongByteArrayList(int initialCapacity, Collection<ByteBuf> values)
    {
        super(initialCapacity, values, DataType.BIGINT::decode);
    }
}
