package com.github.adamorgan.internal.entities;

import com.github.adamorgan.api.entities.Column;

import javax.annotation.Nonnull;

public class ColumnImpl implements Column
{
    private final String keyspace;
    private final String tableName;
    private final String name;
    private final int type;

    public ColumnImpl(String keyspace, String tableName, String name, int type)
    {
        this.keyspace = keyspace;
        this.tableName = tableName;
        this.name = name;
        this.type = type;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return String.format("%s.%s.%s [%s]", keyspace, tableName, name, type);
    }
}
