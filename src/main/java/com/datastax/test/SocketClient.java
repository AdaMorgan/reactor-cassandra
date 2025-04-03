package com.datastax.test;

import com.datastax.api.Library;
import com.datastax.api.exceptions.ErrorResponse;
import com.datastax.api.exceptions.ErrorResponseException;
import com.datastax.api.requests.ObjectAction;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.utils.CustomLogger;
import com.datastax.internal.utils.cache.RequestCacheViewImpl;
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
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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


        public ByteBuf login(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
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
            Map<String, String> map = new HashMap<>();
            map.put(CQL_VERSION_OPTION, CQL_VERSION);
            map.put(DRIVER_VERSION_OPTION, DRIVER_VERSION);
            map.put(DRIVER_NAME_OPTION, DRIVER_NAME);

            EntityBuilder entityBuilder = new EntityBuilder()
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(0x00)
                    .writeShort(0x00)
                    .writeByte(SocketCode.STARTUP);

            ByteBuf body = Unpooled.directBuffer();

            body.writeShort(map.size());

            for (Map.Entry<String, String> entry : map.entrySet())
            {
                writeString(body, entry.getKey());
                writeString(body, entry.getValue());
            }

            entityBuilder.writeBytes(body);

            return entityBuilder.asByteBuf();
        }

        public void writeString(ByteBuf body, String value)
        {
            byte[] bytes = value.getBytes(CharsetUtil.UTF_8);
            body.writeShort(bytes.length);
            body.writeBytes(bytes);
        }

        public ByteBuf registerMessage(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
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

        public ByteBuf createMessageOptions(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
        {
            return new EntityBuilder()
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(0x00)
                    .writeShort(0x00)
                    .writeByte(SocketCode.OPTIONS)
                    .writeInt(0)
                    .asByteBuf();
        }

        public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";
        public static final String TEST_QUERY = "SELECT * FROM system.clients";

        public ByteBuf ready(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
        {
            this.client.library.setStatus(Library.Status.CONNECTED);
            LOG.info("Finished Loading!");

            return new PrepareCreateActionImpl(this.library, 0x04, 0x00, 0x00, ObjectAction.Level.ONE).setContent(TEST_QUERY_PREPARED);
            //return new QueryCreateActionImpl(this.library, 0x04, 0x00, 0x00, ObjectAction.Level.ONE).setContent(TEST_QUERY);
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

        private final RequestCacheViewImpl<ByteBuf> queue = new RequestCacheViewImpl<>(5);

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf frame, List<Object> out)
        {
            ByteBuf compositeFrame = this.queue.isEmpty() || !((this.queue.getLast().getByte(2) & 0x16) == 0x016) ? frame : Unpooled.wrappedBuffer(this.queue.getLast(), frame);

            enqueue(context, compositeFrame, out);
        }

        public void setFlagPage(ByteBuf buffer)
        {
            byte currentByte = buffer.getByte(2);
            int newByte = currentByte | 0x16;
            buffer.setByte(2, newByte);
        }

        public void clearFlagPage(ByteBuf buffer)
        {
            byte currentByte = buffer.getByte(2);
            int newByte = currentByte & ~0x16;
            buffer.setByte(2, newByte);
        }

        public void enqueue(ChannelHandlerContext context, ByteBuf buffer, List<Object> out)
        {
            byte versionHeader = buffer.readByte();

            int version = (256 + versionHeader) & 0x7F;
            boolean isResponse = ((256 + versionHeader) & 0x80) != 0;

            byte flags = buffer.readByte();

            short stream = buffer.readShort();
            byte opcode = buffer.readByte();

            int length = buffer.readInt();

            Supplier<ByteBuf> bufSupplier = () ->
            {
                if (buffer.readableBytes() == length)
                {
                    clearFlagPage(buffer);

                    ByteBuf message = handle(version, isResponse, flags, stream, opcode, length, buffer);

                    if (message != null)
                    {
                        context.writeAndFlush(message.retain());
                    }

                    return buffer.retain();
                }
                else
                {
                    setFlagPage(buffer);
                    buffer.resetReaderIndex();
                    return buffer.copy();
                }
            };

            this.queue.addLast(bufSupplier.get());
        }

        public ByteBuf handle(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
        {
            switch (opcode)
            {
                case SocketCode.ERROR:
                    return this.error(version, isResponse, flags, stream, opcode, length, buffer);
                case SocketCode.AUTHENTICATE:
                    return this.initializer.login(version, isResponse, flags, stream, opcode, length, buffer);
                case SocketCode.AUTH_SUCCESS:
                    return this.initializer.createMessageOptions(version, isResponse, flags, stream, opcode, length, buffer);
                case SocketCode.SUPPORTED:
                    return this.initializer.registerMessage(version, isResponse, flags, stream, opcode, length, buffer);
                case SocketCode.READY:
                    return this.initializer.ready(version, isResponse, flags, stream, opcode, length, buffer);
                case SocketCode.RESULT:
                    boolean isTracing = (flags & 0x02) != 0;
                    return isTracing ? new TracingImpl().read(flags, buffer, this::rowsResult) : rowsResult(buffer);
                default:
                    return null;
            }
        }

        private ByteBuf error(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
        {
            int code = buffer.readInt();

            int lengthMessage = buffer.readUnsignedShort();
            byte[] bytes = new byte[lengthMessage];
            buffer.readBytes(bytes);
            String message = new String(bytes, StandardCharsets.UTF_8);

            ErrorResponse errorResponse = ErrorResponse.fromCode(code);
            throw new ErrorResponseException(errorResponse, buffer, code, message);
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
                    return new ExecuteActionImpl(this.api, 0x04, 0x00, 0x00, ObjectAction.Level.ONE, ObjectAction.Flag.VALUES, ObjectAction.Flag.VALUE_NAMES, ObjectAction.Flag.PAGE_SIZE, ObjectAction.Flag.DEFAULT_TIMESTAMP).executeParameters(buffer);
                case 0x0005:
                    System.out.println("Schema_change: the result to a schema altering query.");
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            return null;
        }
    }
}
