package com.github.adamorgan.test;

import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.data.DataType;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.requests.action.ObjectActionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;

public final class ExecuteActionImpl extends ObjectActionImpl<ByteBuf>
{
    private final ByteBuf response;
    private final int consistency, fields;

    public ExecuteActionImpl(ObjectCreateAction action, Response response)
    {
        super((LibraryImpl) action.getLibrary(), action.getFlagsRaw(), SocketCode.EXECUTE);
        this.response = response.getBody();
        this.consistency = action.getConsistency().getCode();
        this.fields = action.getFieldsRaw();
    }

    @Override
    protected void handleSuccess(Request<ByteBuf> request, Response response)
    {
        request.onSuccess(response.getBody());
    }

    public ByteBuf execute()
    {
        int idLength = this.response.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        this.response.readBytes(preparedId);

        int flags = this.response.readInt();
        System.out.println("flags: " + flags);
        int columnCount = this.response.readInt();
        System.out.println("columnCount: " + columnCount);

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(this.consistency);

        body.writeByte(this.fields);

        //--- size
        body.writeShort(2);

        //--- 1
        writeLongValue(body, 123456L);

        //--- 2
        DataType.LONG_STRING.encode(body, "user");

        //--- flags
        body.writeInt(5000); // page size
        body.writeLong(System.currentTimeMillis()); // timestamp

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }

    @Nonnull
    @Override
    public ByteBuf asByteBuf()
    {
        return execute();
    }

    public ByteBuf executeParameters(ByteBuf buffer)
    {
        int idLength = buffer.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        buffer.readBytes(preparedId);

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.fields)
                .writeShort(0x03)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(this.consistency);

        body.writeByte(this.fields);

        body.writeShort(2);

        DataType.STRING.encode(body, "user_id");
        writeLongValue(body, 123456L);

        DataType.STRING.encode(body, "user_name");
        DataType.LONG_STRING.encode(body, "user");

        body.writeInt(5000);
        body.writeLong(1743025467097000L);

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }

    private void writeLongValue(ByteBuf buf, long value)
    {
        buf.writeInt(8);
        buf.writeLong(value);
    }
}
