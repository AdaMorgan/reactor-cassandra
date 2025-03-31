package com.datastax.test;

import com.datastax.api.Library;
import com.datastax.api.exceptions.ErrorResponse;
import com.datastax.api.exceptions.ErrorResponseException;
import com.datastax.api.requests.ObjectAction;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.utils.CustomLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.collections4.map.LinkedMap;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        context.writeAndFlush(startup.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable failure) throws Exception
    {
        failure.printStackTrace();
    }

    public final class Initializer extends ChannelInitializer<SocketChannel>
    {
        private final Library library;
        private final SocketClient client;
        private final String username, password;

        public Initializer(SocketClient client, String username, String password)
        {
            this.client = client;
            this.library = client.library;
            this.username = username;
            this.password = password;
        }

        @Override
        protected void initChannel(@Nonnull SocketChannel channel) throws Exception
        {
            if (RowsConfig.IS_DEBUG)
            {
                channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            }

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
            return new EntityBuilder().writeByte(PROTOCOL_VERSION)
                    .writeByte(0x00)
                    .writeShort(0x00)
                    .writeByte(SocketCode.STARTUP)
                    .writeString(CQL_VERSION_OPTION, CQL_VERSION, DRIVER_VERSION_OPTION, DRIVER_VERSION, DRIVER_NAME_OPTION, DRIVER_NAME)
                    .asByteBuf();
        }

        public ByteBuf registerMessage()
        {
            this.client.library.setStatus(Library.Status.AWAITING_LOGIN_CONFIRMATION);

            return new EntityBuilder().writeByte(PROTOCOL_VERSION)
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

        public ByteBuf query()
        {
            String query = RowsResultImpl.TEST_QUERY;
            byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);

            int messageLength = 4 + queryBytes.length + 2 + 1;

            return new EntityBuilder(1 + 4 + messageLength)
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(0x02)
                    .writeShort(0x00)
                    .writeByte(SocketCode.QUERY)
                    .writeInt(messageLength)
                    .writeString(RowsResultImpl.TEST_QUERY)
                    .writeShort(0x0001)
                    .writeByte(0x00)
                    .asByteBuf();
        }

        public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";

        public ByteBuf ready()
        {
            this.client.library.setStatus(Library.Status.CONNECTED);
            LOG.info("Finished Loading!");

            return new PrepareActionImpl(this.library, 0x04, 0x00, 0x00, ObjectAction.Level.ONE).execute(TEST_QUERY_PREPARED);
            //return query();
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
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception
        {
            out.add(msg.retain());
        }
    }

    private static final class MessageDecoder extends MessageToMessageDecoder<ByteBuf>
    {
        private final Initializer initializer;
        private final LibraryImpl api;

        public MessageDecoder(Initializer initializer)
        {
            this.api = initializer.client.library;
            this.initializer = initializer;
        }

        private final LinkedMap<ByteBuf, Boolean> history = new LinkedMap<>();

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf frame, List<Object> out)
        {
            ByteBuf compositeFrame = this.history.isEmpty() || this.history.getValue(history.size() - 1) ? frame : Unpooled.wrappedBuffer(this.history.lastKey(), frame);

            processFullFrame(context, compositeFrame, out);
        }

        public void processFullFrame(ChannelHandlerContext context, ByteBuf buffer, List<Object> out)
        {
            byte versionHeader = buffer.readByte();

            int version = (256 + versionHeader) & 0x7F;
            boolean isResponse = ((256 + versionHeader) & 0x80) != 0;

            byte flags = buffer.readByte();
            short stream = buffer.readShort();
            byte opcode = buffer.readByte();
            int length = buffer.readInt();

            if (buffer.readableBytes() == length)
            {
                handle(context, version, isResponse, flags, stream, opcode, length, buffer);
                this.history.put(buffer.retain(), true);
                buffer.release();
            }
            else
            {
                buffer.resetReaderIndex();
                this.history.put(buffer.copy(), false);
            }
        }

        public void handle(ChannelHandlerContext context, int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
        {
            ByteBuf message = handle(version, isResponse, flags, stream, opcode, length, buffer);

            if (message != null)
            {
                context.writeAndFlush(message.retain());
            }
        }

        public ByteBuf handle(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
        {
            switch (opcode)
            {
                case SocketCode.ERROR:
                    return this.error(buffer);
                case SocketCode.AUTHENTICATE:
                    return this.initializer.login();
                case SocketCode.AUTH_SUCCESS:
                    return this.initializer.createMessageOptions();
                case SocketCode.SUPPORTED:
                    return this.initializer.registerMessage();
                case SocketCode.READY:
                    return this.initializer.ready();
                case SocketCode.RESULT:
                    boolean isTracing = (flags & 0x02) != 0;
                    return isTracing ? new TracingImpl().read(flags, buffer, this::rowsResult) : rowsResult(buffer);
                default:
                    return null;
            }
        }

        private ByteBuf rowsResult(ByteBuf buffer)
        {
            int kind = buffer.readInt();

            switch (kind)
            {
                case 0x0001:
                    System.out.println("Void: for results carrying no information.");
                    break;
                case 0x0002:
                    System.out.println("Rows: for results to select queries, returning a set of rows.");
                    new RowsResultImpl(buffer).run();
                    break;
                case 0x0003:
                    System.out.println("Set_keyspace: the result to a `use` query.");
                    break;
                case 0x0004:
                    System.out.println("Prepared: result to a PREPARE message.");
                    return RowsPreparedResultImpl.executeParameters(buffer);
                case 0x0005:
                    System.out.println("Schema_change: the result to a schema altering query.");
                    break;
                default:
                    System.out.println("Received unknown result type: " + kind);
                    break;
            }

            return null;
        }

        private ByteBuf error(ByteBuf buffer) {
            int errorCode = buffer.readInt();
            String message = readString(buffer);

            ErrorResponse errorResponse = ErrorResponse.fromCode(errorCode);
            throw new ErrorResponseException(errorResponse, buffer, errorCode, message);
        }

        private String readString(ByteBuf buffer) {
            int length = buffer.readUnsignedShort();
            byte[] bytes = new byte[length];
            buffer.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
