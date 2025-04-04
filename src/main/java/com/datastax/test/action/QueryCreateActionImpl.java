package com.datastax.test.action;

import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class QueryCreateActionImpl extends ObjectActionImpl<ByteBuf>
{
    protected final String content;
    protected final int level;
    protected final int bitfield;

    public QueryCreateActionImpl(LibraryImpl api, int version, int flags, short stream, String content, Level level, Flag... queryFlags)
    {
        super(api, version, flags, stream, SocketCode.QUERY, null);
        this.content = content;
        this.level = level.getCode();
        this.bitfield = Arrays.stream(queryFlags).mapToInt(Flag::getValue).reduce(0, ((result, original) -> result | original));
    }

    @Override
    public ByteBuf applyData()
    {
        byte[] queryBytes = content.getBytes(StandardCharsets.UTF_8);

        int messageLength = 4 + queryBytes.length + 2 + 1;

        return new EntityBuilder(1 + 4 + messageLength)
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(this.stream)
                .writeByte(this.opcode)
                .writeInt(messageLength)
                .writeString(content)
                .writeShort(this.level)
                .writeByte(this.bitfield)
                .asByteBuf();
    }
}
