package com.datastax.test.action.session;

import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class RegisterActionImpl extends ObjectActionImpl<ByteBuf>
{
    public RegisterActionImpl(LibraryImpl api, byte version, byte flags)
    {
        super(api, version, flags, SocketCode.REGISTER);
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
                .writeString("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE")
                .asByteBuf();
    }
}
