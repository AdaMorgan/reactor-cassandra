package com.datastax.test;

import com.datastax.api.utils.data.DataType;
import com.datastax.internal.entities.ColumnImpl;
import com.datastax.internal.entities.RowImpl;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;
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

    public String run()
    {
        int kind = buffer.readInt();
        int flags = buffer.readInt();

        boolean hasGlobalTableSpec = (flags & 0x0001) != 0;
        boolean hasMorePages = (flags & 0x0002) != 0;
        boolean noMetadata = (flags & 0x0004) != 0;

        int columnsCount = buffer.readInt();

        if (!noMetadata)
        {
            for (int i = 0; i < columnsCount; i++)
            {
                String keyspace = readString(buffer);
                String tableName = readString(buffer);
                String name = readString(buffer);
                DataType type = readType(buffer);

                columns.add(new ColumnImpl(keyspace, tableName, name, type));
            }
        }

        int rowsCount = buffer.readInt();

        for (int rowNumber = 0; rowNumber < rowsCount; rowNumber++)
        {
            for (ColumnImpl column : columns)
            {
                Object value = readValue(buffer, column.getType());
                RowImpl row = new RowImpl(column, value);
                this.rows.addLast(row);
            }
        }

        LinkedList<String> columns = this.columns.stream().map(ColumnImpl::toString).collect(Collectors.toCollection(LinkedList::new));
        LinkedList<String> rows = this.rows.stream().map(RowImpl::toString).collect(Collectors.toCollection(LinkedList::new));

        return new StringUtils.Table(columns, rows).toString();
    }

    @Override
    public String toString()
    {
        return run();
    }

    private static String readString(@Nonnull ByteBuf buffer)
    {
        short length = buffer.readShort();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static DataType readType(@Nonnull ByteBuf buffer)
    {
        int type = buffer.readShort();
        return DataType.fromId(type);
    }

    @Nullable
    private static Object readValue(@Nonnull ByteBuf buffer, DataType type)
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
            case ASCII:
            case VARCHAR:
                return new String(bytes, StandardCharsets.UTF_8);
            case INT:
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getInt();
            case INET:
                return readAddress(bytes);
            case BIGINT:
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getLong();
            case BOOLEAN:
                return bytes[0] != 0;
            case FLOAT:
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getFloat();
            case DOUBLE:
                return ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getDouble();
            case UUID:
            case TIMEUUID:
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
                long mostSignificantBits = byteBuffer.getLong();
                long leastSignificantBits = byteBuffer.getLong();
                return new UUID(mostSignificantBits, leastSignificantBits);
            case TIMESTAMP:
                return new Date(ByteBuffer.wrap(bytes)
                        .order(ByteOrder.BIG_ENDIAN)
                        .getLong());
            case BLOB:
                return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).array();
            default:
                throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    public static InetAddress readAddress(byte[] address)
    {
        try
        {
            return Inet6Address.getByAddress(address);
        }
        catch (UnknownHostException e)
        {
            throw new RuntimeException(e);
        }
    }
}
