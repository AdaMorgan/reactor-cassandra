package com.datastax.internal.utils.codec;

import com.datastax.api.LibraryInfo;
import com.datastax.api.events.session.ReadyEvent;
import com.datastax.api.exceptions.ErrorResponse;
import com.datastax.api.exceptions.ErrorResponseException;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.requests.Work;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.Requester;
import com.datastax.internal.requests.SocketClient;
import com.datastax.internal.requests.SocketCode;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MessageDecoder extends ByteToMessageDecoder
{
    private final LibraryImpl library;
    private final SocketClient client;

    private final byte DEFAULT_FLAG = 0x00;
    private final Requester requester;

    public MessageDecoder(SocketClient.Initializer initializer)
    {
        this.library = (LibraryImpl) initializer.getLibrary();
        this.client = initializer.getClient();
        this.requester = this.library.getRequester();
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out)
    {
        in.markReaderIndex();

        if (in.readableBytes() < 9)
        {
            in.resetReaderIndex();
            return;
        }

        byte versionHeader = in.readByte();

        byte version = (byte) ((256 + versionHeader) & 0x7F);
        boolean isResponse = ((256 + versionHeader) & 0x80) != 0;

        byte flags = in.readByte();
        short stream = in.readShort();
        byte opcode = in.readByte();
        int length = in.readInt();

        if (in.readableBytes() < length)
        {
            in.resetReaderIndex();
            return;
        }

        ByteBuf frame = in.readRetainedSlice(length);

        onDispatch(version, flags, stream, opcode, length, frame, context::writeAndFlush);
    }

    private void onDispatch(byte version, byte flags, short stream, byte opcode, int length, ByteBuf frame, Consumer<? super ByteBuf> callback)
    {
        BiConsumer<ByteBuf, String> writeString = (body, value) -> {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            body.writeShort(bytes.length);
            body.writeBytes(bytes);
        };

        switch (opcode)
        {
            case SocketCode.SUPPORTED:
            {
                new SocketClient.ConnectNode(this.library, () ->
                {
                    Map<String, String> map = new HashMap<>();

                    map.put("CQL_VERSION", LibraryInfo.CQL_VERSION);
                    map.put("DRIVER_VERSION", LibraryInfo.DRIVER_VERSION);
                    map.put("DRIVER_NAME", LibraryInfo.DRIVER_NAME);
                    map.put("THROW_ON_OVERLOAD", LibraryInfo.THROW_ON_OVERLOAD);

                    ByteBuf body = Unpooled.buffer();

                    body.writeShort(map.size());

                    for (Map.Entry<String, String> entry : map.entrySet())
                    {
                        writeString.accept(body, entry.getKey());
                        writeString.accept(body, entry.getValue());
                    }

                    return new EntityBuilder()
                            .writeByte(version)
                            .writeByte(flags)
                            .writeShort(stream)
                            .writeByte(SocketCode.STARTUP)
                            .writeBytes(body)
                            .apply(callback)
                            .asByteBuf();
                });

                break;
            }
            case SocketCode.AUTHENTICATE:
            {
                new SocketClient.ConnectNode(this.library, () ->
                {
                    return new EntityBuilder()
                            .writeByte(version)
                            .writeByte(DEFAULT_FLAG)
                            .writeShort(stream)
                            .writeByte(SocketCode.AUTH_RESPONSE)
                            .writeBytes(this.library.getToken())
                            .apply(callback)
                            .asByteBuf();
                });
                break;
            }
            case SocketCode.AUTH_SUCCESS:
            {
                new SocketClient.ConnectNode(this.library, () ->
                {
                    return new EntityBuilder()
                            .writeByte(version)
                            .writeByte(DEFAULT_FLAG)
                            .writeShort(0x00)
                            .writeByte(SocketCode.REGISTER)
                            .writeString("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE")
                            .apply(callback)
                            .asByteBuf();
                });
                break;
            }
            case SocketCode.READY:
            {
                LibraryImpl.LOG.info("Finished Loading!");
                this.library.handleEvent(new ReadyEvent(this.library));
                break;
            }
            case SocketCode.ERROR:
            {
                throw ErrorResponseException.create(ErrorResponse.fromBuffer(frame), frame);
            }
            default:
            {
                enqueue(version, flags, stream, opcode, length, frame);
            }
        }
    }

    private void enqueue(byte version, byte flags, short stream, byte opcode, int length, ByteBuf frame)
    {
        Queue<Requester.WorkTask> requests = this.requester.requests;

        Consumer<? super Response> consumer = this.requester.queue.remove(stream);

        consumer.accept(new Response(version, flags, stream, opcode, length, frame));

        frame.release();

        if (!requests.isEmpty())
        {
            Requester.WorkTask peek = requests.peek();
            peek.execute();
            requests.remove(peek);
        }
    }
}
