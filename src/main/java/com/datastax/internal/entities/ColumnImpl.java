package com.datastax.internal.entities;

import com.datastax.api.entities.Column;

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
