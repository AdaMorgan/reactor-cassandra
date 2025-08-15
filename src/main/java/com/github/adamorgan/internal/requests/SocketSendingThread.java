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

package com.github.adamorgan.internal.requests;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import javax.annotation.Nonnull;
import java.util.List;

public class SocketSendingThread extends ByteToMessageCodec<ByteBuf>
{
    private final SocketClient client;

    public SocketSendingThread(SocketClient client)
    {
        this.client = client;
    }

    @Override
    protected void encode(@Nonnull ChannelHandlerContext context, @Nonnull ByteBuf request, @Nonnull ByteBuf out)
    {
        out.writeBytes(request.retain());
    }

    @Override
    protected void decode(@Nonnull ChannelHandlerContext context, @Nonnull ByteBuf input, @Nonnull List<Object> out)
    {
        input.markReaderIndex();

        if (input.readableBytes() < 9)
        {
            input.resetReaderIndex();
            return;
        }

        byte versionHeader = input.readByte();

        byte version = (byte) ((256 + versionHeader) & 0x7F);
        boolean isResponse = ((256 + versionHeader) & 0x80) != 0;

        byte flags = input.readByte();
        short stream = input.readShort();
        byte opcode = input.readByte();
        int length = input.readInt();

        if (input.readableBytes() < length)
        {
            input.resetReaderIndex();
            return;
        }

        ByteBuf body = input.readRetainedSlice(length).asReadOnly();

        boolean isCompressed = (flags & 0x01) != 0;

        client.onDispatch(context, version, flags, stream, opcode, length, isCompressed ? client.compression.unpack(body) : body, context::writeAndFlush);
    }
}
