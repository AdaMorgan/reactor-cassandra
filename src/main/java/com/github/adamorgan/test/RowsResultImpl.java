package com.github.adamorgan.test;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class RowsResultImpl
{
    private final ByteBuf buffer;
    private final LinkedList<ColumnImpl> columns = new LinkedList<>();
    private final LinkedList<RowImpl> rows = new LinkedList<>();

    public RowsResultImpl(@Nonnull final ByteBuf buffer)
    {
        this.buffer = buffer;
    }

    public String run() {
        int rowsFlag = buffer.readInt();

        boolean hasGlobalTableSpec = (rowsFlag & 0x0001) != 0;
        boolean hasMorePages = (rowsFlag & 0x0002) != 0;
        boolean noMetadata = (rowsFlag & 0x0004) != 0;

        String keyspace = "";
        String table = "";

        int columnsCount = buffer.readInt();

        if (hasGlobalTableSpec) {
            keyspace = readString(buffer);
            table = readString(buffer);
        }

        if (hasMorePages)
        {
            buffer.skipBytes(buffer.readInt());
        }

        if (!noMetadata) {
            for (int i = 0; i < columnsCount; i++) {
                if (!hasGlobalTableSpec) {
                    keyspace = readString(buffer);
                    table = readString(buffer);
                }

                String name = readString(buffer);
                int type = readType(buffer);

                columns.add(new ColumnImpl(keyspace, table, name, type));
            }
        }

        int rowsCount = buffer.readInt();

        for (int rowNumber = 0; rowNumber < rowsCount; rowNumber++) {
            for (ColumnImpl column : columns) {
                Serializable value = readValue(buffer, column.getType());
                RowImpl row = new RowImpl(column, value);
                this.rows.addLast(row);
            }
        }

        LinkedList<String> columns = this.columns.stream().map(ColumnImpl::toString).collect(Collectors.toCollection(LinkedList::new));
        LinkedList<String> rows = this.rows.stream().map(RowImpl::toString).collect(Collectors.toCollection(LinkedList::new));

        return new StringTableUtils.Table(columns, rows).toString();
    }

    @Override
    public String toString()
    {
        return run();
    }

    private static String readString(@Nonnull ByteBuf buffer)
    {
        int length = buffer.readUnsignedShort();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static int readType(@Nonnull ByteBuf buffer)
    {
        return buffer.readShort();
    }

    private static Serializable readValue(@Nonnull ByteBuf buffer, int type)
    {
        int length = buffer.readInt();
        if (length < 0)
        {
            return Void.class;
        }

        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
