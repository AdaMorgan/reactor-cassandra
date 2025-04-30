package com.github.adamorgan.internal.utils.collections;

import gnu.trove.map.TByteObjectMap;
import gnu.trove.map.hash.TByteByteHashMap;
import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.function.Function;

public class TByteBufArrayList extends TByteArrayList<ByteBuf>
{
    public TByteBufArrayList(Collection<ByteBuf> values, Function<ByteBuf, ? extends ByteBuf> decoder)
    {
        super(values, decoder);
    }
}
