package com.datastax.test;

import com.datastax.api.Library;
import com.datastax.api.requests.ObjectAction;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PrepareCreateActionImpl extends ObjectActionImpl implements ObjectAction
{
    protected final int level;
    protected final int bitfield;

    public PrepareCreateActionImpl(Library api, int version, int flags, int stream, Level level, Flag... queryFlags)
    {
        super(api, version, flags, stream, SocketCode.PREPARE);
        this.level = level.getCode();
        this.bitfield = Arrays.stream(queryFlags).mapToInt(Flag::getValue).reduce(0, ((result, original) -> result | original));
    }

    public ByteBuf setContent(String request)
    {
        byte[] queryBytes = request.getBytes(StandardCharsets.UTF_8);

        int messageLength = 4 + queryBytes.length + 2 + 1;

        return new EntityBuilder(1 + 4 + messageLength)
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(this.stream)
                .writeByte(this.opcode)
                .writeInt(messageLength)
                .writeString(request)
                .writeShort(this.level)
                .writeByte(this.bitfield)
                .asByteBuf();
    }
}
