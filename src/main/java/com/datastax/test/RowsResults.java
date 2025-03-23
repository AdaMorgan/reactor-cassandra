package com.datastax.test;

import com.datastax.api.entities.Column;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RowsResults
{
    public static final String TEST_QUERY = "SELECT * FROM system.raft_state";

    private final ByteBuf buffer;
    private final LinkedList<ColumnImpl> columns = new LinkedList<>();

    public RowsResults(@Nonnull final ByteBuf buffer)
    {
        this.buffer = buffer;
    }

    public void run()
    {
        // Читаем флаги
        int metadataFlags = buffer.readInt();

        boolean hasGlobalTableSpec = (metadataFlags & 0x0001) != 0;
        boolean hasMorePages = (metadataFlags & 0x0002) != 0;
        boolean noMetadata = (metadataFlags & 0x0004) != 0;

        // Читаем количество столбцов
        int columnsCount = buffer.readInt();

        System.out.println("Columns count: " + columnsCount);

        byte[] pagingState;
        if (hasMorePages)
        {
            int pagingStateLength = buffer.readInt();
            pagingState = new byte[pagingStateLength];
            buffer.readBytes(pagingState);
            System.out.println("Has more pages. Paging state: " + Arrays.toString(pagingState));
        }

        if (!noMetadata)
        {
            for (int i = 0; i < columnsCount; i++)
            {
                columns.add(new ColumnImpl(buffer));
            }
        }

        int rowsSize = buffer.readInt();
        System.out.println("Rows size: " + rowsSize);

        for (int rowNumber = 0; rowNumber < rowsSize; rowNumber++)
        {
            for (ColumnImpl column : columns)
            {
                RowImpl row = new RowImpl(column, buffer);
                System.out.println(row);
            }
        }
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

    @Nullable
    private static Object readValue(@Nonnull ByteBuf buffer, String type)
    {
        int length = buffer.readInt();
        if (length < 0)
        {
            return null;
        }

        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);

        switch (type)
        {
            case "ASCII":
            case "TEXT":
            case "VARCHAR":
                return new String(bytes, StandardCharsets.UTF_8);
            case "INT":
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getInt();
            case "BIGINT":
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getLong();
            case "BOOLEAN":
                return bytes[0] != 0;
            case "FLOAT":
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getFloat();
            case "DOUBLE":
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getDouble();
            case "UUID":
            case "TIMEUUID":
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
                long mostSignificantBits = byteBuffer.getLong();
                long leastSignificantBits = byteBuffer.getLong();
                return new UUID(mostSignificantBits, leastSignificantBits);
            case "TIMESTAMP":
                return new Date(ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getLong());
            case "BLOB":
                return bytes; // Возвращаем массив байт как есть
            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    public static class ColumnImpl
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
    }

    public static class RowImpl
    {
        private final ColumnImpl column;
        private final Object value;

        public RowImpl(ColumnImpl column, ByteBuf buffer)
        {
            this.column = column;
            this.value = readValue(buffer, column.type.name());
        }

        @Override
        public String toString()
        {
            return column.toString() + " - " + value;
        }
    }
}
