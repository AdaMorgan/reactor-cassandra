package com.datastax.internal.entities;

import com.datastax.api.entities.Column;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

public class ColumnImpl implements Column
{
    private final String keyspace;
    private final String tableName;
    private final String columnName;
    private final Column.Type type;

    public ColumnImpl(ByteBuf buffer)
    {
        this.keyspace = readString(buffer);
        this.tableName = readString(buffer);
        this.columnName = readString(buffer);
        this.type = readType(buffer);
    }

    @Override
    public String toString()
    {
        return String.format("%s.%s.%s [%s]", keyspace, tableName, columnName, type);
    }

    @Override
    public String getName()
    {
        return tableName;
    }

    @Override
    public Type getType()
    {
        return type;
    }

    private static String readString(@Nonnull ByteBuf buffer)
    {
        short length = buffer.readShort();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Column.Type readType(@Nonnull ByteBuf buffer)
    {
        int type = buffer.readShort();
        return Column.Type.fromId(type);
    }
}
