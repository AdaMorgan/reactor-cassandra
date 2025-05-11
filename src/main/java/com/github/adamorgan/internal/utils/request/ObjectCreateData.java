package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import gnu.trove.impl.hash.THash;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

public class ObjectCreateData
{
    public static final int CONTENT_BYTES = Integer.BYTES;

    protected final LibraryImpl library;
    protected final byte version, opcode;
    protected final int flags;
    protected final short stream;
    protected final int length;
    protected final byte[] content;
    protected final ObjectCreateAction.Consistency consistency;
    protected final int fields;
    protected final int maxBufferSize;
    protected final long nonce;

    public ObjectCreateData(@Nonnull ObjectCreateAction action, byte version, int flags, short stream)
    {
        this.library = (LibraryImpl) action.getLibrary();
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = action.isEmpty() ? SocketCode.QUERY : SocketCode.PREPARE;
        this.content = StringUtils.getBytes(action.getContent(), StandardCharsets.UTF_8);
        this.consistency = action.getConsistency();
        this.fields = action.getFieldsRaw();
        this.length = CONTENT_BYTES + content.length + (this.opcode == SocketCode.QUERY ? Short.BYTES : 0) + ObjectCreateAction.Field.BYTES + ObjectCreateAction.Field.getCapacity(fields);
        this.maxBufferSize = action.getMaxBufferSize();
        this.nonce = action.getNonce();
    }

    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return ObjectCreateAction.Field.fromBitFields(fields);
    }

    public ByteBuf applyData()
    {
        int flags = this.flags;

        flags |= this.library.getCompression().equals(Compression.NONE) ? 0 : 1;

        ByteBuf body = Unpooled.directBuffer()
                .writeInt(content.length)
                .writeBytes(content)
                .writeShort(consistency.getCode())
                .writeByte(fields)
                .writeInt(maxBufferSize)
                .writeLong(nonce)
                .asByteBuf();

        body = this.library.getCompression().pack(body);

        ByteBuf header = Unpooled.directBuffer(length + ObjectAction.HEADER_BYTES)
                .writeByte(version)
                .writeByte(flags)
                .writeShort(stream)
                .writeByte(opcode)
                .writeInt(body.readableBytes())
                .asByteBuf();

        return Unpooled.compositeBuffer().addComponents(true, header, body);
    }
}
