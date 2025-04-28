package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

public class ObjectCreateData
{
    public static final int CONTENT_BYTES = Integer.BYTES;

    protected final LibraryImpl library;
    protected final byte version, flags, opcode;
    protected final short stream;
    protected final int length;
    protected final byte[] content;
    protected final ObjectCreateAction.Consistency consistency;
    protected final int fields;
    protected final int maxBufferSize;
    protected final long nonce;

    public ObjectCreateData(@Nonnull ObjectCreateAction action)
    {
        this.library = (LibraryImpl) action.getLibrary();
        this.version = library.getVersion();
        this.flags = action.getFlagsRaw();
        this.stream = 0x00;
        this.opcode = action.getValues().isEmpty() ? SocketCode.QUERY : SocketCode.PREPARE;
        this.content = StringUtils.getBytes(action.getContent(), StandardCharsets.UTF_8);
        this.consistency = action.getConsistency();
        this.fields = action.getFieldsRaw();
        this.length = CONTENT_BYTES + content.length + ObjectCreateAction.Field.BYTES + ObjectCreateAction.Field.getCapacity(fields);
        this.maxBufferSize = action.getMaxBufferSize();
        this.nonce = action.getNonce();
    }

    public ByteBuf applyData()
    {
        return Unpooled.directBuffer(length + ObjectAction.HEADER_BYTES)
                .writeByte(version)
                .writeByte(flags)
                .writeShort(stream)
                .writeByte(opcode)
                .writeInt(length)
                .writeInt(content.length)
                .writeBytes(content)
                .writeShort(consistency.getCode())
                .writeByte(fields)
                .writeInt(maxBufferSize)
                .writeLong(nonce)
                .asByteBuf();
    }
}
