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

package com.github.adamorgan.internal.utils.codec;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.LibraryInfo;
import com.github.adamorgan.api.exceptions.ErrorResponse;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.Requester;
import com.github.adamorgan.internal.requests.SocketClient;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class MessageDecoder extends ByteToMessageDecoder
{
    private final LibraryImpl library;

    private final Requester requester;
    private final Compression compression;

    public MessageDecoder(LibraryImpl library)
    {
        this.library = library;
        this.requester = this.library.getRequester();
        this.compression = library.getCompression();
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf input, List<Object> out)
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

        onDispatch(version, flags, stream, opcode, length, isCompressed ? this.compression.unpack(body) : body, context::writeAndFlush);
    }

    private void onDispatch(byte version, byte flags, int stream, byte opcode, int length, ByteBuf body, Consumer<? super ByteBuf> callback)
    {
        switch (opcode)
        {
            case SocketCode.SUPPORTED:
            {
                library.setStatus(Library.Status.IDENTIFYING_SESSION);
                sendStartup(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.AUTHENTICATE:
            {
                this.library.setStatus(Library.Status.LOGGING_IN);
                verifyToken(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.AUTH_SUCCESS:
            {
                this.library.setStatus(Library.Status.LOGIN_CONFIRMATION);
                registry(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.READY:
            {
                this.library.setStatus(Library.Status.CONNECTED);
                this.library.getClient().ready();
                break;
            }
            case SocketCode.ERROR:
            case SocketCode.RESULT:
            {
                this.requester.enqueue(version, flags, stream, opcode, length, opcode == SocketCode.ERROR ? ErrorResponse.from(body) : null, body);
                break;
            }
            default:
            {
                throw new UnsupportedOperationException("Unsupported opcode: " + opcode);
            }
        }
    }

    @Nonnull
    private SessionController.SessionConnectNode sendStartup(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.library, () ->
        {
            Map<String, String> map = new HashMap<>();

            map.put("CQL_VERSION", LibraryInfo.CQL_VERSION);
            map.put("DRIVER_VERSION", LibraryInfo.DRIVER_VERSION);
            map.put("DRIVER_NAME", LibraryInfo.DRIVER_NAME);
            map.put("THROW_ON_OVERLOAD", LibraryInfo.THROW_ON_OVERLOAD);

            if (!this.library.getCompression().equals(Compression.NONE))
            {
                map.put("COMPRESSION", this.library.getCompression().toString());
            }

            ByteBuf body = Unpooled.buffer();

            body.writeShort(map.size());

            for (Map.Entry<String, String> entry : map.entrySet())
            {
                EncodingUtils.packUTF84(body, entry.getKey());
                EncodingUtils.packUTF84(body, entry.getValue());
            }
            
            return Unpooled.directBuffer()
                    .writeByte(version)
                    .writeByte(SocketClient.DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.STARTUP)
                    .writeInt(body.readableBytes())
                    .writeBytes(body)
                    .asByteBuf();
        }, callback);
    }

    @Nonnull
    private SessionController.SessionConnectNode verifyToken(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.library, () ->
        {
            byte[] token = this.library.getToken();
            return Unpooled.directBuffer()
                    .writeByte(version)
                    .writeByte(SocketClient.DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.AUTH_RESPONSE)
                    .writeInt(token.length)
                    .writeBytes(token)
                    .asByteBuf();
        }, callback);
    }

    @Nonnull
    private SessionController.SessionConnectNode registry(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.library, () ->
        {
            ByteBuf body = Stream.of("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE").collect(Unpooled::buffer, EncodingUtils::packUTF88, ByteBuf::writeBytes);

            return Unpooled.directBuffer()
                    .writeByte(version)
                    .writeByte(SocketClient.DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.REGISTER)
                    .writeInt(body.readableBytes())
                    .writeBytes(body)
                    .asByteBuf();
        }, callback);
    }
}
