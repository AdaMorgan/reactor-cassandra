/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.internal.requests;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.session.ReadyEvent;
import com.github.adamorgan.api.events.session.SessionDisconnectEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.codec.MessageDecoder;
import com.github.adamorgan.internal.utils.codec.MessageEncoder;
import com.github.adamorgan.internal.utils.config.SessionConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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
    private final LibraryImpl api;
    private final Bootstrap client;
    private final EventLoopGroup executor;
    private final SessionController controller;

    protected final ReentrantLock reconnectLock = new ReentrantLock();
    protected final Condition reconnectCondvar = reconnectLock.newCondition();

    protected boolean shouldReconnect;
    protected boolean connected = false;

    private static final ThreadLocal<ByteBuf> CURRENT_EVENT = new ThreadLocal<>();
    private final SocketAddress address;
    private final Compression compression;

    public SocketClient(LibraryImpl api, SocketAddress address, Compression compression, SessionConfig config)
    {
        this.api = api;
        this.client = config.getClient();
        this.address = address;
        this.compression = compression;
        this.shouldReconnect = api.isAutoReconnect();
        this.executor = api.getCallbackPool();
        this.controller = api.getSessionController();
        this.connectNode = new StartingNode(this, controller::appendSession);
    }

    public class SocketHandler extends ChannelInitializer<SocketChannel>
    {

        @Override
        public void channelActive(@Nonnull ChannelHandlerContext context)
        {
            SocketClient.this.context.set(context);
            sendIdentify(context, context::writeAndFlush);
        }

        @Override
        public void channelInactive(@Nonnull ChannelHandlerContext context)
        {
            connected = false;
            handleDisconnect();
        }

        @Override
        protected void initChannel(@Nonnull SocketChannel channel)
        {
            if (SocketClient.this.api.isDebug())
            {
                channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            }

            channel.pipeline().addLast(new MessageEncoder());
            channel.pipeline().addLast(new MessageDecoder(SocketClient.this.api));
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
        boolean callOnShutdown = MiscUtil.locked(reconnectLock, () ->
        {
            if (connectNode != null)
                api.getSessionController().removeSession(connectNode);
            boolean wasConnected = connected;
            reconnectCondvar.signalAll();
            return !wasConnected;
        });

        if (callOnShutdown)
            onShutdown();
    }

    protected void onShutdown()
    {
        api.shutdownInternals(new ShutdownEvent(api, OffsetDateTime.now()));
    }

    @Nullable
    public ChannelHandlerContext getContext()
    {
        return this.context.get();
    }

    private synchronized SessionController.SessionConnectNode sendIdentify(ChannelHandlerContext context, Consumer<? super ByteBuf> callback)
    {
        LOG.debug("Sending Identify node...");
        return new ConnectNode(this.api, () ->
        {
            return Unpooled.buffer()
                    .writeByte(this.api.getVersion())
                    .writeByte(DEFAULT_FLAG)
                    .writeShort(DEFAULT_STREAM_ID)
                    .writeByte(SocketCode.OPTIONS)
                    .writeInt(0)
                    .asByteBuf();
        }, callback);
    }

    public synchronized void connect() throws IOException
    {
        if (this.api.getStatus() != Library.Status.ATTEMPTING_TO_RECONNECT)
        {
            this.api.setStatus(Library.Status.CONNECTING_TO_SOCKET);
        }

        ChannelFuture connect = connectNode.connect(address).awaitUninterruptibly();

        if (connect.isSuccess())
        {
            connected = true;
            return;
        }

        Throwable failure = connect.cause();

        if (failure instanceof ConnectTimeoutException)
        {
            LOG.debug("Socket timed out");
            return;
        }

        if (failure instanceof ConnectException)
        {
            if (this.api.getStatus() == Library.Status.CONNECTING_TO_SOCKET)
            {
                LOG.error("Cannot create a socket connection");
                this.api.shutdown();
                return;
            }
            else
            {
                throw (IOException) failure.getCause();
            }
        }

        LOG.error("There was an error in the Socket connection", failure);
        this.api.handleEvent(new ExceptionEvent(this.api, failure, true));
    }

    public final void reconnect()
    {
        LOG.debug("Attempting to reconnect in {}s", reconnectTimeoutS);

        boolean isShutdown = MiscUtil.locked(reconnectLock, () -> {
            while (shouldReconnect)
            {
                api.setStatus(Library.Status.WAITING_TO_RECONNECT);

                int delay = reconnectTimeoutS;
                // Exponential backoff, reset on session creation (ready/resume)
                reconnectTimeoutS = reconnectTimeoutS == 0 ? 2 : Math.min(reconnectTimeoutS << 1, api.getMaxReconnectDelay());

                try
                {
                    // On shutdown, this condvar is notified and we stop reconnecting
                    reconnectCondvar.await(delay, TimeUnit.SECONDS);
                    if (!shouldReconnect)
                        break;

                    api.setStatus(Library.Status.ATTEMPTING_TO_RECONNECT);
                    LOG.debug("Attempting to reconnect!");
                    connect();
                    break;
                }
                catch (RejectedExecutionException | InterruptedException failure)
                {
                    return true;
                }
                catch (IOException failure)
                {
                    LOG.debug("Encountered I/O error", failure);
                }
                catch (RuntimeException failure)
                {
                    LOG.debug("Reconnect failed with exception", failure);
                    LOG.warn("Reconnect failed! Next attempt in {}s", reconnectTimeoutS);
                }
            }
            return !shouldReconnect;
        });

        if (isShutdown)
        {
            LOG.debug("Reconnect cancelled due to shutdown.");
            shutdown();
        }
    }

    private void handleDisconnect()
    {
        api.setStatus(Library.Status.DISCONNECTED);
        this.api.getObjectCache().clear();
        if (!shouldReconnect || executor.isShutdown())
        {
            onShutdown();
        }
        else
        {
            api.handleEvent(new SessionDisconnectEvent(this.api, OffsetDateTime.now()));
            reconnect();
        }
    }

    public final void ready()
    {
        this.reconnectTimeoutS = 0;
        LibraryImpl.LOG.info("Finished Loading!");
        this.api.handleEvent(new ReadyEvent(this.api));
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
            this.api = client.api;
            this.callback = callback;
            this.connectNode = SocketClient.this.client.group(executor).channel(CHANNEL_TYPE)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                    .handler(new SocketHandler());
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
