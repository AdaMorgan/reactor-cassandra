package com.github.adamorgan.internal.requests;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.session.SessionDisconnectEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.codec.MessageDecoder;
import com.github.adamorgan.internal.utils.codec.MessageEncoder;
import com.github.adamorgan.test.SocketConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.ConnectException;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SocketClient extends ChannelInboundHandlerAdapter
{
    public static final Logger LOG = LibraryLogger.getLog(SocketClient.class);

    protected final byte version;

    private static final byte DEFAULT_FLAG = 0x00;
    private static final short DEFAULT_STREAM = 0x00;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Bootstrap handler;
    private final LibraryImpl library;
    private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<>();
    private final EventLoopGroup executor;

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

    @Nullable
    public ChannelHandlerContext getContext()
    {
        return this.context.get();
    }

    @Override
    public final void channelActive(ChannelHandlerContext context)
    {
        this.context.set(context);
        this.library.setStatus(Library.Status.IDENTIFYING_SESSION);
        sendIdentify(context, context::writeAndFlush);
    }

    protected synchronized SessionController.SessionConnectNode sendIdentify(ChannelHandlerContext context, Consumer<? super ByteBuf> callback)
    {
        LOG.debug("Sending Identify node...");
        return new ConnectNode(this.library, () -> {
            return Unpooled.directBuffer()
                    .writeByte(this.version)
                    .writeByte(DEFAULT_FLAG)
                    .writeShort(DEFAULT_STREAM)
                    .writeByte(SocketCode.OPTIONS)
                    .writeInt(0)
                    .asByteBuf();
        }, callback);
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
        int delay = reconnectTimeS == 0 ? 2 : Math.min(reconnectTimeS << 1, this.library.getMaxReconnectDelay());

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

    public static final class Initializer extends ChannelInitializer<SocketChannel>
    {

        private final SocketClient client;

        public Initializer(SocketClient client)
        {
            this.client = client;
        }

        public Library getLibrary()
        {
            return client.library;
        }

        public SocketClient getClient()
        {
            return client;
        }

        @Override
        protected void initChannel(@Nonnull SocketChannel channel)
        {
            if (SocketConfig.IS_DEBUG)
            {
                channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            }

            channel.pipeline().addLast(new MessageEncoder(this));

            channel.pipeline().addLast(new MessageDecoder(this));

            channel.pipeline().addLast(new Handler(this.client));
        }
    }

    public static class ConnectNode implements SessionController.SessionConnectNode
    {
        protected final Library api;
        protected final CompletableFuture<Void> handle;

        public ConnectNode(Library api, Supplier<ByteBuf> handle, Consumer<? super ByteBuf> callback)
        {
            this.api = api;
            this.handle = CompletableFuture.supplyAsync(handle).thenAccept(callback);
        }

        @Nonnull
        @Override
        public Library getLibrary()
        {
            return api;
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
