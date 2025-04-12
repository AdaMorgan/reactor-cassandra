package com.datastax.test.action.session;

import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class OptionActionImpl extends ObjectActionImpl<ByteBuf>
{
    public OptionActionImpl(LibraryImpl api, byte flags)
    {
        super(api, flags, SocketCode.OPTIONS);
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
                .writeInt(0)
                .asByteBuf();

    }
}
