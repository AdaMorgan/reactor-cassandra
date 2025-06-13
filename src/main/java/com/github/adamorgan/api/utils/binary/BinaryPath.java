package com.github.adamorgan.api.utils.binary;

public class BinaryPath
{
    protected final String keyspace;
    protected final String table;
    protected final String name;
    protected final int offset;

    public BinaryPath(String keyspace, String table, String name, int type)
    {
        this.keyspace = keyspace;
        this.table = table;
        this.name = name;
        this.offset = type;
    }
}
