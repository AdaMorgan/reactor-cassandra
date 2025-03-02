package com.datastax.api;

import io.netty.buffer.ByteBuf;

public class ColumnImpl implements Column
{
    protected final String name;
    protected final int type;

    public ColumnImpl(String name, ByteBuf info)
    {
        this.name = name;
        this.type = info.readUnsignedShort();
    }

    @Override
    public String getName()
    {
        return this.name;
    }
}
