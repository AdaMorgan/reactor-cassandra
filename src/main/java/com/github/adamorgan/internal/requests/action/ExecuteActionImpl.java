package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.utils.request.ObjectCreateBuilder;
import com.github.adamorgan.internal.utils.requestbody.BinaryType;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.util.*;

public final class ExecuteActionImpl extends ObjectActionImpl<ByteBuf>
{
    private final ByteBuf id;
    private final ObjectCreateActionImpl action;
    private final ByteBuf response;
    private final int consistency, fields, length;

    public ExecuteActionImpl(@Nonnull ObjectCreateActionImpl action, @Nonnull Response response)
    {
        super((LibraryImpl) action.getLibrary(), SocketCode.EXECUTE);
        this.action = action;
        this.response = response.getBody();
        this.consistency = action.getConsistency().getCode();
        this.fields = action.getFieldsRaw();
        this.length = this.response.readUnsignedShort();
        this.id = this.response.readSlice(length);
    }

    @Override
    protected void handleSuccess(Request<ByteBuf> request, Response response)
    {
        this.action.handleSuccess(request, response);
    }

    @Nonnull
    public ByteBuf execute()
    {
        int fields = this.fields;

        fields |= ObjectCreateAction.Field.VALUE_NAMES.getRawValue();

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer()
                .writeShort(this.id.readableBytes())
                .writeBytes(this.id)
                .writeShort(this.consistency)
                .writeByte(fields);
                //.writeBytes(this.action.builder.getBody())

        body.writeShort(2);

        BinaryType.STRING.pack(body, "user_id");
        BinaryType.BIGINT.pack(body, 123456L);

        BinaryType.STRING.pack(body, "user_name");
        BinaryType.LONG_STRING.pack(body, "reganjohn");

        body
                .writeInt(this.action.getMaxBufferSize())
                .writeLong(this.action.getNonce());

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

    public ByteBuf executeParameters()
    {
        int fields = this.fields;

        fields |= ObjectCreateAction.Field.VALUE_NAMES.getRawValue();

        int idLength = this.response.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        this.response.readBytes(preparedId);

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(this.consistency);

        body.writeByte(fields);

        body.writeShort(2);

        BinaryType.STRING.pack(body, "user_id");
        writeLongValue(body, 123456L);

        BinaryType.STRING.pack(body, "user_name");
        BinaryType.LONG_STRING.pack(body, "reganjohn");

        body.writeInt(5000);
        body.writeLong(System.currentTimeMillis());

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
