package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCallbackAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public class ObjectCallbackData implements ObjectData
{
    private final ObjectCallbackAction action;
    private final byte version, opcode;
    private final int flags;
    private final int stream;
    private final ByteBuf token, body;
    private final ByteBuf header;
    private final short consistency;
    private final int fields, maxBufferSize;
    private final long nonce;

    public ObjectCallbackData(@Nonnull ObjectCallbackAction action, byte version, int stream)
    {
        this.action = action;
        this.version = version;
        this.flags = action.getRawFlags();
        this.stream = stream;
        this.opcode = SocketCode.EXECUTE;
        this.token = action.getToken();
        this.consistency = action.getConsistency().getCode();
        this.fields = action.getFieldsRaw();
        this.maxBufferSize = action.getMaxBufferSize();
        this.nonce = action.getNonce();
        this.body = action.getCompression().pack(applyBody());
        this.header = applyHeader();
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return ObjectCreateAction.Field.fromBitFields(fields);
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        return Unpooled.wrappedBuffer(header, body);
    }

    private ByteBuf applyHeader()
    {
        return Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(this.stream)
                .writeByte(this.opcode)
                .writeInt(body.readableBytes())
                .asReadOnly();
    }

    private ByteBuf applyBody()
    {
        return Unpooled.directBuffer()
                .writeShort(this.token.readableBytes())
                .writeBytes(this.token)
                .writeShort(this.consistency)
                .writeByte(this.fields)
                .writeBytes(action.getBody())
                .writeInt(this.maxBufferSize)
                .writeLong(this.nonce)
                .asByteBuf();
    }
}
