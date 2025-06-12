package com.github.adamorgan.internal.requests;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.session.SessionDisconnectEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.codec.MessageDecoder;
import com.github.adamorgan.internal.utils.codec.MessageEncoder;
import com.github.adamorgan.internal.utils.config.SessionConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SocketClient
{
    public static final Logger LOG = LibraryLogger.getLog(SocketClient.class);

    public static final byte DEFAULT_FLAG = 0x00;
    public static final int DEFAULT_STREAM_ID = 0x00;

    private int reconnectTimeoutS = 0;

    public static final Class<? extends SocketChannel> CHANNEL_TYPE = Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;

    private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<>();

    private final StartingNode connectNode;
    private final LibraryImpl library;
    private final Bootstrap client;
    private final EventLoopGroup executor;
    private final SessionController controller;

    private static final ThreadLocal<ByteBuf> CURRENT_EVENT = new ThreadLocal<>();
    private final SocketAddress address;
    private final Compression compression;

    public SocketClient(LibraryImpl library, SocketAddress address, Compression compression, SessionConfig config)
    {
        this.library = library;
        this.client = config.getClient();
        this.address = address;
        this.compression = compression;
        this.executor = library.getCallbackPool();
        this.controller = library.getSessionController();
        this.connectNode = new StartingNode(this, controller::appendSession);
    }

    public class SocketHandler extends ChannelInitializer<SocketChannel>
    {
        @Override
        public void channelActive(@Nonnull ChannelHandlerContext context)
        {
            SocketClient.this.context.set(context);
            library.setStatus(Library.Status.IDENTIFYING_SESSION);
            sendIdentify(context, context::writeAndFlush);
        }

        @Override
        public void channelInactive(@Nonnull ChannelHandlerContext context)
        {
            library.setStatus(Library.Status.DISCONNECTED);
            library.handleEvent(new SessionDisconnectEvent(library, OffsetDateTime.now()));
            reconnect(reconnectTimeoutS);
        }

        @Override
        protected void initChannel(@Nonnull SocketChannel channel)
        {
            if (library.isDebug())
            {
                channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            }

            channel.pipeline().addLast(new MessageEncoder());
            channel.pipeline().addLast(new MessageDecoder(library));
            channel.pipeline().addLast(new ChannelInboundHandlerAdapter()
            {
                @Override
                public void channelActive(@Nonnull ChannelHandlerContext ctx)
                {
                    SocketHandler.this.channelActive(ctx);
                }

                @Override
                public void channelInactive(@Nonnull ChannelHandlerContext ctx)
                {
                    SocketHandler.this.channelInactive(ctx);
                }
            });
        }
    }

    @Nonnull
    public Compression getCompression()
    {
        return compression;
    }

    public synchronized void shutdown()
    {
        this.library.setStatus(Library.Status.SHUTDOWN);
        this.library.handleEvent(new ShutdownEvent(this.library, OffsetDateTime.now()));

        this.executor.shutdownGracefully().addListener(future -> {
            if (this.connectNode != null)
                this.controller.removeSession(this.connectNode);
        });
    }

    @Nullable
    public ChannelHandlerContext getContext()
    {
        return this.context.get();
    }

    private synchronized SessionController.SessionConnectNode sendIdentify(ChannelHandlerContext context, Consumer<? super ByteBuf> callback)
    {
        LOG.debug("Sending Identify node...");
        return new ConnectNode(this.library, () ->
        {
            return Unpooled.directBuffer()
                    .writeByte(this.library.getVersion())
                    .writeByte(DEFAULT_FLAG)
                    .writeShort(DEFAULT_STREAM_ID)
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

        ChannelFuture future = connectNode.connect(address);

        future.awaitUninterruptibly();

        if (future.isSuccess())
        {
            this.library.setStatus(Library.Status.CONNECTED);
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
                this.library.shutdown();
                return;
            }

            throw new RuntimeException(failure);
        }

        LOG.error("There was an error in the Socket connection", failure);
        this.library.handleEvent(new ExceptionEvent(this.library, failure, true));
    }

    public final void reconnect(int reconnectTimeS)
    {
        int delay = reconnectTimeoutS = reconnectTimeS == 0 ? 2 : Math.min(reconnectTimeS << 1, this.library.getMaxReconnectDelay());

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
            return this.api;
        }

        @Nonnull
        public CompletableFuture<Void> getFuture()
        {
            return handle;
        }
    }

    public class StartingNode implements SessionController.SessionConnectNode
    {
        private final LibraryImpl api;
        private final Bootstrap connectNode;
        private final Consumer<StartingNode> callback;

        public StartingNode(SocketClient client, Consumer<StartingNode> callback)
        {
            this.api = client.library;
            this.callback = callback;
            this.connectNode = SocketClient.this.client.group(executor).channel(CHANNEL_TYPE).handler(new SocketHandler());
        }

        @Nonnull
        @Override
        public Library getLibrary()
        {
            return api;
        }

        @Nonnull
        public ChannelFuture connect(SocketAddress inetSocketAddress)
        {
            return this.connectNode.connect(inetSocketAddress).addListener(future -> {
                this.callback.accept(this);
            });
        }
    }
}
