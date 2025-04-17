package com.datastax.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;

import java.io.InputStream;
import java.sql.ResultSet;
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
        ResultSet resultSet;

        InputStream stream;

        return this.buffer;
    }
}
