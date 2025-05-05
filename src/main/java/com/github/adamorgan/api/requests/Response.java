package com.github.adamorgan.api.requests;

import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class Response
{
    protected final int version, flags, stream, opcode, length;
    protected final ByteBuf body;
    protected final Type type;

    public Response(byte version, byte flags, short stream, byte opcode, int length, ByteBuf body)
    {
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = opcode;
        this.length = length;
        this.body = body;
        this.type = Type.valueOf(body.readInt());
    }

    public boolean isOk()
    {
        return this.opcode != SocketCode.ERROR;
    }

    public boolean isError()
    {
        return this.opcode == SocketCode.ERROR;
    }

    public boolean isTrace()
    {
        return (flags & 0x02) != 0;
    }

    public boolean isWarnings()
    {
        return (flags & 0x08) != 0;
    }

    @Nonnull
    public ByteBuf getBody()
    {
        return body;
    }

    @Nonnull
    public Type getType()
    {
        return type;
    }

    public enum Type
    {
        VOID(0x0001),
        ROWS(0x0002),
        SET_KEYSPACE(0x0003),
        PREPARED(0x0004),
        SCHEMA_CHANGE(0x0005);

        private final int offset;

        Type(int offset)
        {
            this.offset = offset;
        }

        public int getOffset()
        {
            return offset;
        }

        @Nonnull
        public static Type valueOf(final int type)
        {
            for (Type value : values())
            {
                if (value.offset == type)
                {
                    return value;
                }
            }

            throw new UnsupportedOperationException("unexpected result type: " + type);
        }
    }
}
