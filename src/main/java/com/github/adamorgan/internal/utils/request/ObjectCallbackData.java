package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCallbackAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ObjectCallbackData
{
    private final ObjectCallbackAction action;
    private final byte version, opcode;
    private final int flags;
    private final short stream;
    private final ByteBuf token, body;
    private final ByteBuf header;
    private final short consistency;
    private final int fields, maxBufferSize;
    private final long nonce;
    private final Compression compression;

    public ObjectCallbackData(ObjectCallbackAction action, byte version, int flags, short stream)
    {
        this.action = action;
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = SocketCode.EXECUTE;
        this.token = action.getToken();
        this.consistency = action.getConsistency().getCode();
        this.fields = action.getFieldsRaw();
        this.maxBufferSize = action.getMaxBufferSize();
        this.nonce = action.getNonce();
        this.compression = action.getLibrary().getCompression();
        this.body = this.compression.pack(applyBody());
        this.header = applyHeader();
    }

    public ByteBuf applyData()
    {
        return Unpooled.compositeBuffer().addComponents(true, header, body);
    }

    public ByteBuf applyHeader()
    {
        return Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(finalizeFlags())
                .writeShort(this.stream)
                .writeByte(this.opcode)
                .writeInt(body.readableBytes())
                .asByteBuf();
    }

    public ByteBuf applyBody()
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

    private int finalizeFlags()
    {
        return this.flags | (compression.equals(Compression.NONE) ? 0 : 1);
    }
}
