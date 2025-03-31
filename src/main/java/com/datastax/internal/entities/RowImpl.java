package com.datastax.internal.entities;

import com.datastax.api.entities.Column;
import com.datastax.api.entities.Row;
import com.datastax.test.RowsResultImpl;
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
import java.util.UUID;

public class RowImpl implements Row
{
    private final Column column;
    private final Object value;

    public RowImpl(Column column, ByteBuf buffer)
    {
        this.column = column;
        this.value = readValue(buffer, column.getType().name());
    }

    @Override
    public String toString()
    {
        return value != null ? value.toString() : "null";
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
            case "INET":
                return readAddress(bytes);
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
