/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.api.requests;

import com.github.adamorgan.api.exceptions.ErrorResponse;
import com.github.adamorgan.api.exceptions.ErrorResponseException;
import com.github.adamorgan.api.utils.binary.BinaryArray;
import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class Response
{
    protected final int version, flags, stream, opcode, length;
    protected final ByteBuf body;
    protected final Type type;

    protected final UUID trace;

    protected final Exception exception;

    public Response(byte version, byte flags, int stream, byte opcode, int length, ErrorResponse error, ByteBuf body, UUID trace)
    {
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = opcode;
        this.length = length;
        this.body = body;

        this.type = error == null ? Type.valueOf(body.readInt()) : Type.ERROR;
        this.exception = error != null ? ErrorResponseException.create(error, this) : null;

        this.trace = trace;
    }

    public boolean isOk()
    {
        return this.exception == null;
    }

    public boolean isError()
    {
        return this.opcode == SocketCode.ERROR;
    }

    public boolean isEmpty()
    {
        return this.body.readableBytes() == 0 || !type.equals(Type.ROWS) || isError();
    }

    public boolean isTrace()
    {
        return (flags & 0x02) != 0;
    }

    public boolean isWarnings()
    {
        return (flags & 0x08) != 0;
    }

    @Nullable
    public UUID getTrace()
    {
        return this.trace;
    }

    @Nonnull
    public ByteBuf getBody()
    {
        return body;
    }

    @Nullable
    public BinaryArray getArray()
    {
        return !isEmpty() ? new BinaryArray(body) : null;
    }

    @Nullable
    public Exception getException()
    {
        return exception;
    }

    @Nonnull
    public Type getType()
    {
        return type;
    }

    public enum Type
    {
        ERROR(0x0000), // not included in CQL Protocol
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
