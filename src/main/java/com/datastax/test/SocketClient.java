package com.datastax.test;

import com.datastax.api.Library;
import com.datastax.api.exceptions.ErrorResponse;
import com.datastax.api.exceptions.ErrorResponseException;
import com.datastax.api.requests.ObjectAction;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.utils.CustomLogger;
import com.datastax.internal.utils.cache.RequestCacheViewImpl;
import com.datastax.test.action.ExecuteActionImpl;
import com.datastax.test.action.QueryCreateActionImpl;
import com.datastax.test.action.session.LoginCreateActionImpl;
import com.datastax.test.action.session.OptionActionImpl;
import com.datastax.test.action.session.RegisterActionImpl;
import com.datastax.test.action.session.StartingActionImpl;
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
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.datastax.test.SocketClient.Initializer.TEST_QUERY;

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
    private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<>();

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
        this.library.setStatus(Library.Status.IDENTIFYING_SESSION);
        this.context.set(context);

        short starting = 0;
        new StartingActionImpl(this.library, PROTOCOL_VERSION, 0x00, starting, null).queue(node -> {
            System.out.println("starting!");
            short login = 1;
            new LoginCreateActionImpl(this.library, PROTOCOL_VERSION, 0x00, login, null).queue(authSuccess -> {
                System.out.println("authSuccess!");
                short option = 2;
                new OptionActionImpl(this.library, PROTOCOL_VERSION, 0x00, option, null).queue(supported -> {
                    System.out.println("supported!");
                    short register = 3;
                    new RegisterActionImpl(this.library, PROTOCOL_VERSION, 0x00, register, null).queue(ready -> {
                        System.out.println("ready!");
                        short query = 4;
                        new QueryCreateActionImpl(this.library, PROTOCOL_VERSION, 0x00, query, TEST_QUERY, ObjectAction.Level.ONE).queue(buf -> {
                            System.out.println("RESULT!");
                        });
                    });
                });
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable failure) throws Exception
    {
        failure.printStackTrace();
    }

    private final Map<Short, Consumer<? super Response>> queue = new HashMap<>();

    public <R> void execute(Request<R> request)
    {
        ChannelHandlerContext context = this.context.get();

        ByteBuf body = request.getBody();

        if (context != null && body != null)
        {
            ByteBuf buf = request.applyData() == null ? new EntityBuilder()
                    .writeByte(PROTOCOL_VERSION)
                    .writeByte(request.getFlags())
                    .writeShort(request.getStreamId())
                    .writeByte(request.getCode())
                    .writeBytes(body)
                    .asByteBuf() : request.applyData();

            request.handleResponse(this.queue::put);

            context.writeAndFlush(buf.retain());
        }
    }

    public final class Initializer extends ChannelInitializer<SocketChannel>
    {
        private final LibraryImpl library;
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

        public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";
        public static final String TEST_QUERY = "SELECT * FROM system.clients";
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

        private final RequestCacheViewImpl<ByteBuf> cacheRequest = new RequestCacheViewImpl<>(5);

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf frame, List<Object> out)
        {
            enqueue(context, frame, out);
        }

        public void setFlagPage(ByteBuf buffer)
        {
            byte currentByte = buffer.getByte(1);
            int newByte = currentByte | 0x16;
            buffer.setByte(1, newByte);
        }

        public void enqueue(ChannelHandlerContext context, ByteBuf frame, List<Object> out)
        {
            ByteBuf compositeFrame = this.cacheRequest.isEmpty() || !((this.cacheRequest.getLast().getByte(1) & 0x16) == 0x016) ? frame : Unpooled.wrappedBuffer(this.cacheRequest.getLast(), frame);

            byte versionHeader = compositeFrame.readByte();

            byte version = (byte) ((256 + versionHeader) & 0x7F);
            boolean isResponse = ((256 + versionHeader) & 0x80) != 0;

            byte flags = compositeFrame.readByte();
            short stream = compositeFrame.readShort();
            byte opcode = compositeFrame.readByte();
            int length = compositeFrame.readInt();

            ByteBuf buf = directBuffer(version, isResponse, flags, stream, opcode, length, compositeFrame);

            this.cacheRequest.addLast(buf);
        }

        private ByteBuf directBuffer(byte version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf frame)
        {
            if (frame.readableBytes() == length)
            {
                flags = (byte) (flags & ~0x16);

                handle(version, isResponse, flags, stream, opcode, length, frame);

                Consumer<? super Response> consumer = this.initializer.client.queue.get(stream);

                consumer.accept(new Response(version, flags, stream, opcode, length, frame));

                this.initializer.client.queue.remove(stream);

                return frame.retain();
            }
            else
            {
                setFlagPage(frame);
                frame.resetReaderIndex();
                return frame.copy();
            }
        }

        public void handle(int version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf buffer)
        {
            switch (opcode)
            {
                case SocketCode.ERROR:
                    this.error(version, isResponse, flags, stream, opcode, length, buffer);
                    return;
                case SocketCode.RESULT:
                    boolean isTracing = (flags & 0x02) != 0;
                    ByteBuf buf = isTracing ? new TracingImpl().read(flags, buffer, this::rowsResult) : rowsResult(buffer);
                default:
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
                    short stream = 0;
                    return new ExecuteActionImpl(this.api, 0x04, 0x00, stream, ObjectAction.Level.ONE, ObjectAction.Flag.VALUES, ObjectAction.Flag.VALUE_NAMES, ObjectAction.Flag.PAGE_SIZE, ObjectAction.Flag.DEFAULT_TIMESTAMP).executeParameters(buffer);
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
