package com.datastax.internal.requests;

import com.datastax.api.Library;
import com.datastax.api.events.ExceptionEvent;
import com.datastax.api.events.session.ReadyEvent;
import com.datastax.api.events.session.SessionDisconnectEvent;
import com.datastax.api.events.session.ShutdownEvent;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.utils.SessionController;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.utils.LibraryLogger;
import com.datastax.test.EntityBuilder;
import com.datastax.test.SocketConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

public class SocketClient extends ChannelInboundHandlerAdapter
{
    public static final Logger LOG = LibraryLogger.getLog(SocketClient.class);

    protected final byte version;

    private static final byte DEFAULT_FLAG = 0x00;
    private static final byte DEFAULT_STREAM = 0x00;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Bootstrap handler;
    private final LibraryImpl library;
    private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<>();
    private final EventLoopGroup executor;

    private final Map<Short, Consumer<? super Response>> queue = new ConcurrentHashMap<>();
    private final Queue<Request<?>> cacheRequest = new ConcurrentLinkedQueue<>();

    private static final ThreadLocal<ByteBuf> CURRENT_EVENT = new ThreadLocal<>();

    public SocketClient(LibraryImpl library)
    {
        this.executor = new NioEventLoopGroup();
        this.library = library;
        this.version = library.getVersion();
        this.handler = new Bootstrap()
                .group(executor)
                .channel(NioSocketChannel.class)
                .handler(new Initializer(this))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context)
    {
        this.library.setStatus(Library.Status.DISCONNECTED);
        this.library.handleEvent(new SessionDisconnectEvent(this.library, OffsetDateTime.now()));
        reconnect(0);
    }

    public synchronized void shutdown()
    {
        this.library.setStatus(Library.Status.SHUTDOWN);
        this.library.handleEvent(new ShutdownEvent(this.library, OffsetDateTime.now()));
        this.executor.shutdownGracefully();
    }

    @Override
    public final void channelActive(ChannelHandlerContext context)
    {
        this.context.set(context);
        this.library.setStatus(Library.Status.IDENTIFYING_SESSION);
        sendIdentify(context, context::writeAndFlush);
    }

    protected ByteBuf sendIdentify(ChannelHandlerContext context, Consumer<? super ByteBuf> callback)
    {
        LOG.debug("Sending Identify node...");
        return new ConnectNode(this.library, () ->
        {
            return new EntityBuilder().writeByte(this.version)
                    .writeByte(DEFAULT_FLAG)
                    .writeShort(DEFAULT_STREAM)
                    .writeByte(SocketCode.OPTIONS)
                    .writeInt(0)
                    .requireHandler(callback)
                    .asByteBuf();
        }).asByteBuf();
    }

    public synchronized void connect()
    {
        if (this.library.getStatus() != Library.Status.ATTEMPTING_TO_RECONNECT)
        {
            this.library.setStatus(Library.Status.CONNECTING_TO_SOCKET);
        }

        ChannelFuture future = handler.connect(HOST, PORT);
        future.awaitUninterruptibly();

        if (future.isSuccess())
        {
            return;
        }

        Throwable failure = future.cause();

        if (failure instanceof ConnectTimeoutException)
        {
            LOG.debug("Socket timed out");
            return;
        }

        if (failure instanceof ConnectException)
        {
            if (this.library.getStatus() == Library.Status.CONNECTING_TO_SOCKET)
            {
                LOG.error("Cannot create a socket connection");
                this.shutdown();
                return;
            }

            throw new RuntimeException(failure);
        }

        LOG.error("There was an error in the Socket connection", failure);
        this.library.handleEvent(new ExceptionEvent(this.library, failure, true));
    }

    public final void reconnect(int reconnectTimeS)
    {
        int delay = reconnectTimeS == 0 ? 2 : Math.min(reconnectTimeS << 1, 64);

        LOG.debug("Reconnect attempt in {}s", delay);

        this.library.setStatus(Library.Status.WAITING_TO_RECONNECT);

        this.executor.schedule(() ->
        {
            try
            {
                this.library.setStatus(Library.Status.ATTEMPTING_TO_RECONNECT);
                LOG.debug("Attempting to reconnect!");
                this.connect();
            }
            catch (RuntimeException failure)
            {
                LOG.warn("Reconnect failed! Next attempt in {}s", delay);
                reconnect(delay);
            }
        }, delay, TimeUnit.SECONDS);
    }

    public <R> void execute(Request<R> request, short stream)
    {
        ChannelHandlerContext context = this.context.get();

        if (context != null && !this.queue.containsKey(stream))
        {
            ByteBuf body = request.getBody();

            body.setShort(2, stream);

            request.handleResponse(stream, this.queue::put);

            context.writeAndFlush(body.retain());
        }
        else
        {
            cacheRequest.add(request);
        }
    }

    public static final class Initializer extends ChannelInitializer<SocketChannel>
    {

        private final SocketClient client;

        public Initializer(SocketClient client)
        {
            this.client = client;
        }

        @Override
        protected void initChannel(@Nonnull SocketChannel channel)
        {
            if (SocketConfig.IS_DEBUG)
            {
                channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            }

            channel.pipeline().addLast(new MessageDecoder(this));
            channel.pipeline().addLast(new MessageEncoder());

            channel.pipeline().addLast(new Handler(this.client));
        }
    }

    private static final class MessageEncoder extends MessageToMessageEncoder<ByteBuf>
    {
        @Override
        protected void encode(ChannelHandlerContext ctx, @Nonnull ByteBuf msg, @Nonnull List<Object> out)
        {
            out.add(msg.retain());
        }
    }

    private static final class MessageDecoder extends ByteToMessageDecoder
    {
        private final Initializer initializer;
        private final LibraryImpl library;

        public MessageDecoder(Initializer initializer)
        {
            this.library = initializer.client.library;
            this.initializer = initializer;
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
            switch (opcode)
            {
                case SocketCode.SUPPORTED:
                {
                    BiConsumer<ByteBuf, String> writeString = (body, value) -> {
                        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                        body.writeInt(bytes.length);
                        body.writeBytes(bytes);
                    };

                    new ConnectNode(this.library, () ->
                    {
                        Map<String, String> map = new HashMap<>();
                        map.put("CQL_VERSION_OPTION", "4.0.0");
                        map.put("DRIVER_VERSION_OPTION", "0.2.0");
                        map.put("DRIVER_NAME_OPTION", "mmorrii");
                        map.put("THROW_ON_OVERLOAD_OPTION", "true");

                        ByteBuf body = Unpooled.directBuffer();

                        body.writeShort(map.size());

                        for (Map.Entry<String, String> entry : map.entrySet())
                        {
                            writeString.accept(body, entry.getKey());
                            writeString.accept(body, entry.getValue());
                        }

                        return new EntityBuilder()
                                .writeByte(version)
                                .writeByte(DEFAULT_FLAG)
                                .writeShort(stream)
                                .writeByte(SocketCode.STARTUP)
                                .writeBytes(body)
                                .requireHandler(callback)
                                .asByteBuf();
                    }).asByteBuf();
                    break;
                }
                case SocketCode.AUTHENTICATE:
                {
                    new ConnectNode(this.library, () ->
                    {
                        String username = "cassandra";
                        String password = "cassandra";
                        return new EntityBuilder()
                                .writeByte(version)
                                .writeByte(DEFAULT_FLAG)
                                .writeShort(stream)
                                .writeByte(SocketCode.AUTH_RESPONSE)
                                .writeString(username, password)
                                .requireHandler(callback)
                                .asByteBuf();
                    }).asByteBuf();
                    break;
                }
                case SocketCode.AUTH_SUCCESS:
                {
                    new ConnectNode(this.library, () ->
                    {
                        return new EntityBuilder()
                                .writeByte(version)
                                .writeByte(DEFAULT_FLAG)
                                .writeShort(0x00)
                                .writeByte(SocketCode.REGISTER)
                                .writeString("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE")
                                .requireHandler(callback)
                                .asByteBuf();
                    }).asByteBuf();
                    break;
                }
                case SocketCode.READY:
                {
                    LibraryImpl.LOG.info("Finished Loading!");
                    this.library.handleEvent(new ReadyEvent(this.library));
                    break;
                }
                default:
                {
                    enqueue(version, flags, stream, opcode, length, frame);
                }
            }
        }

        private void enqueue(byte version, byte flags, short stream, byte opcode, int length, ByteBuf frame)
        {
            Queue<Request<?>> cacheRequest = this.initializer.client.cacheRequest;

            Consumer<? super Response> consumer = this.initializer.client.queue.remove(stream);

            consumer.accept(new Response(version, flags, stream, opcode, length, frame));

            frame.release();

            if (!cacheRequest.isEmpty())
            {
                Request<?> peek = cacheRequest.peek();
                this.library.getRequester().execute(peek);
                cacheRequest.remove(peek);
            }
        }
    }

    public static class ConnectNode implements SessionController.SessionConnectNode
    {
        protected final Library api;

        protected final Supplier<ByteBuf> body;

        public ConnectNode(Library api, Supplier<ByteBuf> body)
        {
            this.api = api;
            this.body = body;
        }

        @Override
        public Library getLibrary()
        {
            return api;
        }

        @Override
        public ByteBuf asByteBuf()
        {
            return this.body.get();
        }

        @Override
        public String toString()
        {
            return ByteBufUtil.prettyHexDump(asByteBuf());
        }
    }

    private static final class Handler extends ChannelInboundHandlerAdapter
    {
        private final SocketClient client;

        public Handler(SocketClient client)
        {
            this.client = client;
        }

        @Override
        public void channelInactive(@Nonnull ChannelHandlerContext ctx) throws Exception
        {
            this.client.channelInactive(ctx);
        }

        @Override
        public void channelActive(@Nonnull ChannelHandlerContext ctx) throws Exception
        {
            this.client.channelActive(ctx);
        }
    }
}
