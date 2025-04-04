package com.datastax.test.action.session;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

import java.util.function.BiFunction;

public class RegisterActionImpl extends ObjectActionImpl<ByteBuf>
{
    public RegisterActionImpl(LibraryImpl api, int version, int flags, short stream, BiFunction<Request<ByteBuf>, Response, ByteBuf> handler)
    {
        super(api, version, flags, stream, SocketCode.REGISTER, handler);
    }

    @Override
    public ByteBuf finalizeBuffer()
    {
        return new EntityBuilder().writeString("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE").asByteBuf();
    }
}
