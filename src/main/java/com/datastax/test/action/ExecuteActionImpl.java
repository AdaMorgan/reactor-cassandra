package com.datastax.test.action;

import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ExecuteActionImpl extends ObjectActionImpl<ByteBuf>
{
    private final int level;
    private final int executeFlags;

    public ExecuteActionImpl(LibraryImpl api, int version, int flags, short stream, Level level, Flag... executeFlags)
    {
        super(api, version, flags, stream, SocketCode.EXECUTE, null);
        this.level = level.getCode();
        this.executeFlags = Arrays.stream(executeFlags).mapToInt(Flag::getValue).reduce(0, ((result, original) -> result | original));
    }

    public ByteBuf execute(ByteBuf buffer)
    {
        int idLength = buffer.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        buffer.readBytes(preparedId);

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(this.stream)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(this.level);

        body.writeByte(this.executeFlags);

        body.writeShort(2);

        writeLongValue(body, 123456);
        writeString(body, "user", EntityBuilder.TypeTag.INT);

        body.writeInt(5000);
        body.writeLong(1743025467097000L);

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }

    public ByteBuf executeParameters(ByteBuf buffer)
    {
        int idLength = buffer.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        buffer.readBytes(preparedId);

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(this.stream)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(this.level);

        body.writeByte(this.executeFlags);

        body.writeShort(2);

        writeString(body, "user_id", EntityBuilder.TypeTag.SHORT);
        writeLongValue(body, 123456L);

        writeString(body, "user_name", EntityBuilder.TypeTag.SHORT);
        writeString(body, "user", EntityBuilder.TypeTag.INT);

        body.writeInt(5000); // page_size
        body.writeLong(1743025467097000L); //default timestamp

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }

    private static void writeString(ByteBuf buf, String value, EntityBuilder.TypeTag tag)
    {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        tag.writeLock(buf, bytes.length);
        buf.writeBytes(bytes);

        //buf.nioBuffer().position(bytes.length).asCharBuffer().put(value);
    }

    private static void writeLongValue(ByteBuf buf, long value)
    {
        buf.writeInt(8);
        buf.writeLong(value);
    }
}
