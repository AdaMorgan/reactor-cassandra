package com.datastax.test.action.session;

import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RegisterActionImpl extends ObjectActionImpl<ByteBuf>
{
    private final String[] types;

    public RegisterActionImpl(LibraryImpl api, byte flags, EventType... types)
    {
        super(api, flags, SocketCode.REGISTER);
        this.types = Arrays.stream(types).map(Enum::toString).toArray(String[]::new);
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        return new EntityBuilder()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode)
                .writeString(this.types)
                .asByteBuf();
    }

    public enum EventType
    {
        SCHEMA_CHANGE,
        TOPOLOGY_CHANGE,
        STATUS_CHANGE
    }
}
