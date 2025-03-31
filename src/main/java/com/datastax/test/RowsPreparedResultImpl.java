package com.datastax.test;

import com.datastax.api.requests.ObjectAction;
import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class RowsPreparedResultImpl implements ObjectAction
{
    private final static byte PROTOCOL_VERSION = 0x04;

    public static ByteBuf execute(ByteBuf buffer)
    {
        int idLength = buffer.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        buffer.readBytes(preparedId);

        ByteBuf buf = Unpooled.buffer()
                .writeByte(PROTOCOL_VERSION)
                .writeByte(0x00)
                .writeShort(0x00)
                .writeByte(SocketCode.EXECUTE);

        ByteBuf body = Unpooled.buffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(Level.ONE.getCode());

        writeFlags(body, Flag.VALUES, Flag.PAGE_SIZE, Flag.DEFAULT_TIMESTAMP);

        body.writeShort(2);

        writeLongValue(body, 123456);
        writeString(body, "user");

        body.writeInt(5000);
        body.writeLong(1743025467097000L);

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }

    public static ByteBuf executeParameters(ByteBuf buffer)
    {
        int idLength = buffer.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        buffer.readBytes(preparedId);

        ByteBuf buf = Unpooled.buffer()
                .writeByte(PROTOCOL_VERSION)
                .writeByte(0x0)
                .writeShort(0x00)
                .writeByte(SocketCode.EXECUTE);

        ByteBuf body = Unpooled.buffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(Level.ONE.getCode());

        writeFlags(body, Flag.VALUES, Flag.PAGE_SIZE, Flag.DEFAULT_TIMESTAMP, Flag.VALUE_NAMES);

        body.writeShort(2);

        writeString(body, "user_id");
        writeLongValue(body, 123456L);

        writeString(body, "user_name");
        writeStringValue(body, "user");

        body.writeInt(5000); // page_size
        body.writeLong(1743025467097000L); //default timestamp

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }

    private static void writeString(ByteBuf buf, String value)
    {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    private static void writeStringValue(ByteBuf buf, String value)
    {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static void writeLongValue(ByteBuf buf, long value)
    {
        buf.writeInt(8);
        buf.writeLong(value);
    }

    private static void writeFlags(ByteBuf body, Flag... flags)
    {
        int bitfield = 0;

        for (Flag flag : flags)
        {
            bitfield |= flag.getValue();
        }

        body.writeByte(bitfield);
    }
}
