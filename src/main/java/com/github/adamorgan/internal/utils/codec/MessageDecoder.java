package com.github.adamorgan.internal.utils.codec;

import com.github.adamorgan.api.LibraryInfo;
import com.github.adamorgan.api.events.session.ReadyEvent;
import com.github.adamorgan.api.exceptions.ErrorResponse;
import com.github.adamorgan.api.exceptions.ErrorResponseException;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.Requester;
import com.github.adamorgan.internal.requests.SocketClient;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
    private final SocketClient client;

    private final byte DEFAULT_FLAG = 0x00;
    private final Requester requester;

    public MessageDecoder(SocketClient.ReliableFrameHandler handler)
    {
        this.library = (LibraryImpl) handler.getLibrary();
        this.client = handler.getClient();
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

    private synchronized void onDispatch(byte version, byte flags, short stream, byte opcode, int length, ByteBuf frame, Consumer<? super ByteBuf> callback)
    {
        switch (opcode)
        {
            case SocketCode.SUPPORTED:
            {
                sendStartup(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.AUTHENTICATE:
            {
                verifyToken(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.AUTH_SUCCESS:
            {
                registry(version, flags, stream, opcode, length, callback);
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
                this.requester.enqueue(version, flags, stream, opcode, length, frame);
            }
        }
    }

    @Nonnull
    private SessionController.SessionConnectNode sendStartup(byte version, byte flags, short stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.library, () ->
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
                EncodingUtils.encodeUTF84(body, entry.getKey());
                EncodingUtils.encodeUTF84(body, entry.getValue());
            }

            return Unpooled.directBuffer()
                    .writeByte(version)
                    .writeByte(DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.STARTUP)
                    .writeInt(body.readableBytes())
                    .writeBytes(body)
                    .asByteBuf();
        }, callback);
    }

    @Nonnull
    private SessionController.SessionConnectNode verifyToken(byte version, byte flags, short stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.library, () -> {
            byte[] token = this.library.getToken();

            return Unpooled.directBuffer()
                    .writeByte(version)
                    .writeByte(DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.AUTH_RESPONSE)
                    .writeInt(token.length)
                    .writeBytes(token)
                    .asByteBuf();
        }, callback);
    }

    @Nonnull
    private SessionController.SessionConnectNode registry(byte version, byte flags, short stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.library, () -> {
            ByteBuf body = Stream.of("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE").collect(Unpooled::directBuffer, EncodingUtils::encodeUTF88, ByteBuf::writeBytes);

            return Unpooled.directBuffer()
                    .writeByte(version)
                    .writeByte(DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.REGISTER)
                    .writeInt(body.readableBytes())
                    .writeBytes(body)
                    .asByteBuf();
        }, callback);
    }
}
