package com.datastax.test.action.session;

import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class LoginCreateActionImpl extends ObjectActionImpl<ByteBuf>
{
    public LoginCreateActionImpl(LibraryImpl api, byte version, byte flags)
    {
        super(api, version, flags, SocketCode.AUTH_RESPONSE);
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        String username = "cassandra";
        String password = "cassandra";
        return new EntityBuilder()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode)
                .writeString(username, password)
                .asByteBuf();
    }
}
