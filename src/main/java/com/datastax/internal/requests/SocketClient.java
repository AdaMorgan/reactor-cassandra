package com.datastax.internal.requests;

import com.datastax.api.Library;
import com.datastax.api.LibraryInfo;
import com.datastax.api.events.ExceptionEvent;
import com.datastax.api.events.session.ReadyEvent;
import com.datastax.api.events.session.SessionDisconnectEvent;
import com.datastax.api.events.session.ShutdownEvent;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.utils.SessionController;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.internal.utils.LibraryLogger;
import com.datastax.test.EntityBuilder;
import com.datastax.test.RowsResultImpl;
import com.datastax.test.SocketConfig;
import com.datastax.test.action.Level;
import com.datastax.test.action.ObjectCreateActionImpl;
import com.datastax.test.action.session.LoginCreateActionImpl;
import com.datastax.test.action.session.OptionActionImpl;
import com.datastax.test.action.session.RegisterActionImpl;
import com.datastax.test.action.session.StartingActionImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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
        this.handler = new Bootstrap().group(executor)
                .channel(NioSocketChannel.class)
                .handler(new Initializer(this))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception
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

        new OptionActionImpl(this.library, DEFAULT_FLAG).queue(supported ->
        {
            new StartingActionImpl(this.library, DEFAULT_FLAG).queue(node ->
            {
                new LoginCreateActionImpl(this.library, DEFAULT_FLAG).queue(authSuccess ->
                {
                    new RegisterActionImpl(this.library, DEFAULT_FLAG, RegisterActionImpl.EventType.SCHEMA_CHANGE, RegisterActionImpl.EventType.SCHEMA_CHANGE, RegisterActionImpl.EventType.TOPOLOGY_CHANGE).queue(ready ->
                    {
                        LibraryImpl.LOG.info("Finished Loading!");
                        this.library.handleEvent(new ReadyEvent(this.library));
                    });
                });
            });
        });
    }

    private BiFunction<Byte, ByteBuf, SessionController.SessionConnectNode> onDispatch()
    {
        return (opcode, body) -> {
            switch (opcode)
            {
                case SocketCode.SUPPORTED:
                {
                    return new ConnectNode(this.library, version, DEFAULT_FLAG, DEFAULT_STREAM, SocketCode.STARTUP, () ->
                    {
                        return new EntityBuilder().asByteBuf();
                    });
                }
                case SocketCode.AUTHENTICATE:
                {
                    return new ConnectNode(this.library, version, DEFAULT_FLAG, DEFAULT_STREAM, SocketCode.AUTH_RESPONSE, () ->
                    {
                        return new EntityBuilder().asByteBuf();
                    });
                }
                case SocketCode.AUTH_SUCCESS:
                {
                    return new ConnectNode(this.library, version, DEFAULT_FLAG, DEFAULT_STREAM, SocketCode.REGISTER, () ->
                    {
                        return new EntityBuilder().asByteBuf();
                    });
                }
                case SocketCode.READY:
                {
                    LibraryImpl.LOG.info("Finished Loading!");
                    this.library.handleEvent(new ReadyEvent(this.library));
                    return null;
                }
                default:
                    return null;
            }
        };
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
                channel.pipeline()
                        .addLast(new LoggingHandler(LogLevel.INFO));
            }

            channel.pipeline()
                    .addLast(new MessageDecoder(this));
            channel.pipeline()
                    .addLast(new MessageEncoder());

            channel.pipeline()
                    .addLast(new Handler(this.client));
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
        private final LibraryImpl api;

        public MessageDecoder(Initializer initializer)
        {
            this.api = initializer.client.library;
            this.initializer = initializer;
        }

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf frame, List<Object> out)
        {
            enqueue(context, frame, out);
        }

        public void enqueue(ChannelHandlerContext context, ByteBuf in, List<Object> out)
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

            directBuffer(context, version, isResponse, flags, stream, opcode, length, frame);
        }

        private void directBuffer(ChannelHandlerContext context, byte version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf frame)
        {
            Queue<Request<?>> cacheRequest = this.initializer.client.cacheRequest;

            Consumer<? super Response> consumer = this.initializer.client.queue.remove(stream);

            consumer.accept(new Response(version, flags, stream, opcode, length, frame));

            frame.release();

            if (!cacheRequest.isEmpty())
            {
                Request<?> peek = cacheRequest.peek();
                this.api.getRequester()
                        .execute(peek);
                cacheRequest.remove(peek);
            }
        }
    }

    public static class ConnectNode implements SessionController.SessionConnectNode
    {
        protected final Library api;

        protected final byte version, flags, opcode;
        protected final short stream;

        protected final Callable<ByteBuf> body;

        public ConnectNode(Library api, byte version, byte flags, short stream, byte opcode, Callable<ByteBuf> body)
        {
            this.api = api;
            this.version = version;
            this.flags = flags;
            this.stream = stream;
            this.opcode = opcode;
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
            return new EntityBuilder().writeByte(this.version)
                    .writeByte(this.flags)
                    .writeShort(this.stream)
                    .writeByte(this.opcode)
                    .writeBytes(this.body)
                    .asByteBuf();
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
