package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCallbackAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ObjectCallbackData
{
    public static final int CONTENT_BYTES = Integer.BYTES;

    private final byte version, opcode;
    private final int flags;
    private final short stream;
    private final ByteBuf token, body;
    private final short consistency;
    private final int fields, maxBufferSize;
    private final long nonce;

    public ObjectCallbackData(ObjectCallbackAction action, byte version, int flags, short stream)
    {
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = SocketCode.EXECUTE;
        this.token = action.getToken();
        this.body = action.getBody();
        this.consistency = action.getConsistency().getCode();
        this.fields = action.getFieldsRaw();
        this.maxBufferSize = action.getMaxBufferSize();
        this.nonce = action.getNonce();
    }

    public ByteBuf applyData()
    {
        int fields = this.fields;

        fields |= ObjectCreateAction.Field.VALUE_NAMES.getRawValue();

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(flags)
                .writeShort(this.stream)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer()
                .writeShort(this.token.readableBytes())
                .writeBytes(this.token)
                .writeShort(this.consistency)
                .writeByte(this.fields)
                .writeBytes(this.body)
                .writeInt(this.maxBufferSize)
                .writeLong(this.nonce)
                .asByteBuf();

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        return buf;
    }
}
