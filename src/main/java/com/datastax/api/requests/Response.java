package com.datastax.api.requests;

import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;

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

    public ByteBuf getBody()
    {
        return body;
    }
}
