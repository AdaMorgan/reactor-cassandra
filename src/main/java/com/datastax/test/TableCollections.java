package com.datastax.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;

import java.util.Collection;
import java.util.LinkedList;

public class TableCollections<T> extends LinkedList<T> implements Collection<T>, ByteBufConvertible
{
    private final ByteBuf buffer;

    public TableCollections(ByteBuf buffer)
    {
        this.buffer = buffer;
    }

    @Override
    public ByteBuf asByteBuf()
    {
        return this.buffer;
    }
}
