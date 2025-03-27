package com.datastax.test;

import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class RowsPreparedResults
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";

    private final static byte PROTOCOL_VERSION = 0x04;

    public static ByteBuf prepare()
    {
        byte[] queryBytes = TEST_QUERY_PREPARED.getBytes(StandardCharsets.UTF_8);

        int messageLength = 4 + queryBytes.length + 2 + 1;

        ByteBuf byteBuf = Unpooled.buffer(1 + 4 + messageLength);

        byteBuf.writeByte(PROTOCOL_VERSION);
        byteBuf.writeByte(0x00);
        byteBuf.writeShort(0x00);
        byteBuf.writeByte(SocketCode.PREPARE);
        byteBuf.writeInt(messageLength);
        byteBuf.writeInt(queryBytes.length);
        byteBuf.writeBytes(queryBytes);
        byteBuf.writeShort(0x0001);
        byteBuf.writeByte(0x00);

        return byteBuf;
    }

    public static ByteBuf execute(ByteBuf buffer)
    {
        int idLength = buffer.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        buffer.readBytes(preparedId);

        ByteBuf buf = Unpooled.buffer()
                .writeByte(PROTOCOL_VERSION)
                .writeByte(0x00)
                .writeShort(0x00)  // stream ID
                .writeByte(SocketCode.EXECUTE);

        ByteBuf body = Unpooled.buffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(1);

        int flags = 0;

        flags |= 0x01;
        flags |= 0x04;
        flags |= 0x20;

        body.writeByte(flags);

        body.writeShort(2); // count parameters

        writeLong(body, 123456);
        writeString(body, "user");

        body.writeInt(5000); // page_size
        body.writeLong(1743025467097000L); //default timestamp

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
                .writeByte(0x00)
                .writeShort(0x00)  // stream ID
                .writeByte(SocketCode.EXECUTE);

        ByteBuf body = Unpooled.buffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(1);

        int flags = 0;

        flags |= 0x01;
        flags |= 0x04;
        flags |= 0x20;
        flags |= 0x40;

        body.writeByte(flags);

        // 5. Именованные параметры
        // Сначала записываем количество параметров
        body.writeShort(2); // 2 параметра

        // Затем пары [имя, значение] для каждого параметра
        // Параметр 1: user_id (long)
        writeString(body, "user_id");
        writeLong(body, 123456L);

        // Параметр 2: user_name (string)
        writeString(body, "user_name");
        writeString(body, "user");

        body.writeInt(5000); // page_size
        body.writeLong(1743025467097000L); //default timestamp

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }

    public static void writeLong(ByteBuf cb, long value)
    {
        ByteBuf buf = Unpooled.buffer(8).writeLong(value);

        cb.writeInt(buf.readableBytes());
        cb.writeBytes(buf);
    }

    public static void writeString(ByteBuf cb, String value)
    {
        ByteBuf buf = Unpooled.wrappedBuffer(value.getBytes(StandardCharsets.UTF_8));

        cb.writeInt(buf.readableBytes());
        cb.writeBytes(buf);
    }

    public enum QueryFlag
    {
        VALUES(            0x01),
        SKIP_METADATA(     0x02),
        PAGE_SIZE(         0x04),
        PAGING_STATE(      0x08),
        SERIAL_CONSISTENCY(0x10),
        DEFAULT_TIMESTAMP( 0x20),
        VALUE_NAMES(       0x40);

        private final int mask;

        QueryFlag(int mask)
        {
            this.mask = mask;
        }

        public int getMask()
        {
            return mask;
        }
    }
}
