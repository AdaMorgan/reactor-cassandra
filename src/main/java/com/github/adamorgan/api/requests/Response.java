package com.github.adamorgan.api.requests;

import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class Response
{
    protected final int version, flags, stream, opcode, length;
    protected final ByteBuf body;

    public Response(byte version, byte flags, short stream, byte opcode, int length, ByteBuf body)
    {
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = opcode;
        this.length = length;
        this.body = body;
    }

    public boolean isOk()
    {
        return this.opcode != SocketCode.ERROR;
    }

    public boolean isError()
    {
        return this.opcode == SocketCode.ERROR;
    }

    @Nonnull
    public ByteBuf getBody()
    {
        return body;
    }
}
