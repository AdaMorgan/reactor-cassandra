package com.datastax.test;

import com.datastax.api.Library;
import com.datastax.api.exceptions.ErrorResponse;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.entities.EntityBuilder;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.utils.CustomLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SocketClient extends ChannelInboundHandlerAdapter
{
    public static final Logger LOG = CustomLogger.getLog(SocketClient.class);

    private static final byte PROTOCOL_VERSION = 0x04;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Initializer initializer;
    private final Bootstrap handler;
    private final LibraryImpl library;
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;

    public SocketClient(LibraryImpl library)
    {
        this.library = library;
        this.bootstrap = new Bootstrap();
        this.group = new NioEventLoopGroup();
        this.initializer = new Initializer(this, "cassandra", "cassandra");
        this.handler = this.bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(this.initializer)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true);
    }

    public synchronized void connect()
    {
        handler.connect(HOST, PORT);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext context) throws Exception
    {
        this.library.setStatus(Library.Status.CONNECTING_TO_SOCKET);
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception
    {
        ByteBuf startup = this.initializer.createStartupMessage();
        this.library.setStatus(Library.Status.IDENTIFYING_SESSION);
        context.writeAndFlush(startup.retain()).get(5, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable failure) throws Exception
    {
        failure.printStackTrace();
    }

    public final class Initializer extends ChannelInitializer<SocketChannel>
    {
        private final SocketClient client;
        private final String username, password;

        public Initializer(SocketClient client, String username, String password)
        {
            this.client = client;
            this.username = username;
            this.password = password;
        }

        @Override
        protected void initChannel(SocketChannel channel) throws Exception
        {
            //channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));

            channel.pipeline().addLast(new MessageDecoder(this));
            channel.pipeline().addLast(new MessageEncoder(this));

            channel.pipeline().addLast(this.client);
        }

        private static final String CQL_VERSION_OPTION = "CQL_VERSION";
        private static final String CQL_VERSION = "3.0.0";

        private static final String DRIVER_VERSION_OPTION = "DRIVER_VERSION";
        private static final String DRIVER_VERSION = "0.2.0";

        private static final String DRIVER_NAME_OPTION = "DRIVER_NAME";
        private static final String DRIVER_NAME = "mmorrii one love!";

        static final String COMPRESSION_OPTION = "COMPRESSION";
        static final String NO_COMPACT_OPTION = "NO_COMPACT";


        public ByteBuf login()
        {
            this.client.library.setStatus(Library.Status.LOGGING_IN);

            return new EntityBuilder()
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(0x00)
                    .writeShort(0x00)
                    .writeByte(SocketCode.AUTH_RESPONSE)
                    .writeString(this.username, this.password)
                    .asByteBuf();
        }

        public ByteBuf createStartupMessage()
        {
            return new EntityBuilder()
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(0x00)
                    .writeShort(0x00)
                    .writeByte(SocketCode.STARTUP)
                    .writeEntry(CQL_VERSION_OPTION, CQL_VERSION, DRIVER_VERSION_OPTION, DRIVER_VERSION, DRIVER_NAME_OPTION, DRIVER_NAME)
                    .asByteBuf();
        }

        public ByteBuf registerMessage()
        {
            this.client.library.setStatus(Library.Status.AWAITING_LOGIN_CONFIRMATION);

            return new EntityBuilder()
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(0x00)
                    .writeShort(0x00)
                    .writeByte(SocketCode.REGISTER)
                    .writeString("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE")
                    .asByteBuf();
        }

        public ByteBuf createMessageOptions()
        {
            return new EntityBuilder()
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(0x00)
                    .writeShort(0x00)
                    .writeByte(SocketCode.OPTIONS)
                    .writeInt(0)
                    .asByteBuf();
        }

        public ByteBuf testBuf() {
            String query = "SELECT * FROM system.clients";
            byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);

            int messageLength = 4 + queryBytes.length + 2 + 1;

            ByteBuf buffer = Unpooled.buffer(1 + 4 + messageLength);

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(SocketCode.QUERY);

            buffer.writeInt(messageLength);

            buffer.writeInt(queryBytes.length);
            buffer.writeBytes(queryBytes);

            buffer.writeShort(0x0001);
            buffer.writeByte(0x00);

            return buffer;
        }

        public ByteBuf ready()
        {
            this.client.library.setStatus(Library.Status.CONNECTED);
            LOG.info("Finished Loading!");

            return null;
        }
    }

    private static final class MessageEncoder extends MessageToMessageEncoder<ByteBuf>
    {
        private final Initializer initializer;

        public MessageEncoder(Initializer initializer)
        {
            this.initializer = initializer;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
        {
            out.add(byteBuf);
        }
    }

    private static final class MessageDecoder extends MessageToMessageDecoder<ByteBuf>
    {
        private final Initializer initializer;
        private final LibraryImpl api;
        private final Deque<ByteBuf> queue = new LinkedList<>();

        public MessageDecoder(Initializer initializer)
        {
            this.api = initializer.client.library;
            this.initializer = initializer;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf frame, List<Object> out) throws Exception
        {
            this.queue.addLast(frame);
            processFullFrame(ctx, frame, out);
        }

        private ByteBuf processResultResponse(ByteBuf buffer)
        {
            int kind = buffer.readInt();

            switch (kind)
            {
                case 0x0001:
                    System.out.println("Void: for results carrying no information.");
                    break;
                case 0x0002:
                    System.out.println("Rows: for results to select queries, returning a set of rows.");
                    break;
                case 0x0003:
                    System.out.println("Set_keyspace: the result to a `use` query.");
                    break;
                case 0x0004:
                    System.out.println("Prepared: result to a PREPARE message.");
                    break;
                case 0x0005:
                    System.out.println("Schema_change: the result to a schema altering query.");
                    break;
                default:
                    System.out.println("Received unknown result type: " + kind);
                    break;
            }

            return null;
        }

        public ByteBuf handle(int opcode, ByteBuf buffer, int streamId)
        {
            switch (opcode)
            {
                case SocketCode.ERROR:
                    return this.error(buffer, streamId);
                case SocketCode.AUTHENTICATE:
                    return this.initializer.login();
                case SocketCode.AUTH_SUCCESS:
                    return this.initializer.createMessageOptions();
                case SocketCode.SUPPORTED:
                    return this.initializer.registerMessage();
                case SocketCode.READY:
                    return this.initializer.ready();
                case SocketCode.RESULT:
                    return this.processResultResponse(buffer);
                default:
                    return null;
            }
        }

        public void processFullFrame(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out)
        {
            byte versionHeaderByte = byteBuf.readByte();
            int version = (256 + versionHeaderByte) & 0x7F;
            boolean isResponse = ((256 + versionHeaderByte) & 0x80) != 0;

            byte flags = byteBuf.readByte();
            short streamId = byteBuf.readShort();
            byte opcode = byteBuf.readByte();
            int length = byteBuf.readInt();

            ByteBuf message = handle(opcode, byteBuf, streamId);

            if (message != null)
            {
                ctx.writeAndFlush(message);
            }

            byteBuf.resetReaderIndex();
            out.add(byteBuf.retain());
        }

        private ByteBuf error(ByteBuf buffer, int streamId)
        {
            int codeError = buffer.readInt();
            String message = buffer.readCharSequence(buffer.readUnsignedShort(), StandardCharsets.UTF_8).toString();

            throw new RuntimeException(streamId + " || " + ErrorResponse.fromCode(codeError).name() + ": " + message);
        }
    }
}
