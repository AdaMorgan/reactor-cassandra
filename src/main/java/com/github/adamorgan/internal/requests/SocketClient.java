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
import com.github.adamorgan.api.LibraryInfo;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.session.*;
import com.github.adamorgan.api.exceptions.ErrorResponse;
import com.github.adamorgan.api.exceptions.ErrorResponseException;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.EncodingUtils;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.config.ThreadingConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SocketClient extends ChannelInboundHandlerAdapter implements Closeable
{
    public static final Logger LOG = LibraryLogger.getLog(SocketClient.class);

    public static final byte DEFAULT_FLAG = 0x00;
    public static final int DEFAULT_STREAM_ID = 0x00;

    protected final LibraryImpl api;
    protected final Library.ShardInfo shardInfo;
    protected final Compression compression;

    protected int reconnectTimeoutS = 2;

    protected ChannelHandlerContext context;

    protected final EventLoopGroup executor;
    protected Bootstrap bootstrap;
    protected final SessionController controller;

    protected volatile Future<?> keepAliveThread;

    protected final ReentrantLock reconnectLock = new ReentrantLock();
    protected final Condition reconnectCondvar = reconnectLock.newCondition();

    protected boolean initiating;

    protected int missedHeartbeats = 0;
    protected long heartbeatStartTime;

    protected volatile boolean shutdown = false;
    protected boolean shouldReconnect;
    protected boolean identify = false;
    protected boolean connected = false;

    protected boolean firstInit = true;
    protected boolean processingReady = true;

    private static final ThreadLocal<ByteBuf> CURRENT_EVENT = new ThreadLocal<>();
    private final SocketAddress address;

    protected volatile ConnectNode connectNode;

    public SocketClient(@Nonnull LibraryImpl api, SocketAddress address, Compression compression)
    {
        this.api = api;
        this.shardInfo = api.getShardInfo();
        this.compression = compression;
        this.address = address;
        this.shouldReconnect = api.isAutoReconnect();
        this.executor = api.getCallbackPool();
        this.controller = api.getSessionController();
        this.connectNode = new StartingNode();

        try
        {
            api.getSessionController().appendSession(connectNode);
        }
        catch (RuntimeException | Error error)
        {
            LOG.error("Failed to append new session to session controller queue. Shutting down!", error);
            this.api.setStatus(Library.Status.SHUTDOWN);
            this.api.handleEvent(new ShutdownEvent(api, OffsetDateTime.now()));
            if (error instanceof RuntimeException)
                throw (RuntimeException) error;
            else
                throw (Error) error;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext context)
    {
        LOG.info("Connected to WebSocket");
        if (!identify)
            LOG.debug("Sending Identify-packet...");

        context.pipeline().addLast(new SocketSendingThread(this));
        this.context = context;
        sendIdentify(context::writeAndFlush);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context)
    {
        connected = false;
        handleDisconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable failure)
    {
        failure.printStackTrace();
    }

    protected void onDispatch(ChannelHandlerContext context, byte version, byte flags, int stream, byte opCode, int length, ByteBuf body, Consumer<? super ByteBuf> callback)
    {
        switch (opCode)
        {
            case SocketCode.SUPPORTED:
            {
                if (initiating)
                {
                    identify = true;
                    api.setStatus(Library.Status.IDENTIFYING_SESSION);
                    sendStartup(version, flags, stream, opCode, length, callback);
                }
                else
                {
                    LOG.trace("Got Heartbeat Ack.");
                    missedHeartbeats = 0;
                    api.setGatewayPing(System.currentTimeMillis() - heartbeatStartTime);
                }
                break;
            }
            case SocketCode.AUTHENTICATE:
            {
                this.api.setStatus(Library.Status.AWAITING_LOGIN_CONFIRMATION);
                verifyToken(version, flags, stream, opCode, length, callback);
                break;
            }
            case SocketCode.AUTH_SUCCESS:
            {
                LOG.info("Login Successful!");
                registry(version, flags, stream, opCode, length, callback);
                break;
            }
            case SocketCode.READY:
            {
                this.api.setStatus(Library.Status.CONNECTED);
                ready();
                setupKeepAlive();
                break;
            }
            case SocketCode.ERROR:
                ErrorResponse errorResponse = ErrorResponse.from(body);
                ErrorResponseException exception = ErrorResponseException.create(errorResponse, body);
                this.api.getRequester().handleResponse(context, flags, stream, opCode, length, exception, body);
                break;
            case SocketCode.RESULT:
            {
                this.api.getRequester().handleResponse(context, flags, stream, opCode, length, null, body);
                break;
            }
            default:
            {
                LOG.debug("Got unknown op-code: {} with content: {}", opCode, ByteBufUtil.hexDump(body));
            }
        }
    }

    protected void setupSendingThread()
    {
        bootstrap = new Bootstrap().group(executor)
                .channel(ThreadingConfig.SOCKET_CHANNEL)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                .handler(this)
                .validate();
    }

    @Override
    public boolean isSharable()
    {
        return true;
    }

    private void sendStartup(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        Map<String, String> map = new HashMap<>();

        map.put("CQL_VERSION", LibraryInfo.CQL_VERSION);
        map.put("DRIVER_VERSION", LibraryInfo.DRIVER_VERSION);
        map.put("DRIVER_NAME", LibraryInfo.DRIVER_NAME);
        map.put("THROW_ON_OVERLOAD", LibraryInfo.THROW_ON_OVERLOAD);

        if (!this.compression.equals(Compression.NONE))
        {
            map.put("COMPRESSION", this.api.getCompression().toString());
        }

        ByteBuf body = Unpooled.buffer();

        body.writeShort(map.size());

        for (Map.Entry<String, String> entry : map.entrySet())
        {
            EncodingUtils.packUTF84(body, entry.getKey());
            EncodingUtils.packUTF84(body, entry.getValue());
        }

        ByteBuf request = Unpooled.buffer().writeByte(version)
                .writeByte(SocketClient.DEFAULT_FLAG)
                .writeShort(stream)
                .writeByte(SocketCode.STARTUP)
                .writeInt(body.readableBytes())
                .writeBytes(body)
                .asByteBuf();

        context.writeAndFlush(request.retain());
    }

    private void verifyToken(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        byte[] token = this.api.getToken();
        ByteBuf request = Unpooled.buffer()
                .writeByte(version)
                .writeByte(SocketClient.DEFAULT_FLAG)
                .writeShort(stream)
                .writeByte(SocketCode.AUTH_RESPONSE)
                .writeInt(token.length)
                .writeBytes(token)
                .asByteBuf();

        context.writeAndFlush(request.retain());
    }

    private void registry(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        ByteBuf body = Stream.of("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE").collect(Unpooled::buffer, EncodingUtils::packUTF88, ByteBuf::writeBytes);

        ByteBuf request = Unpooled.buffer()
                .writeByte(version)
                .writeByte(SocketClient.DEFAULT_FLAG)
                .writeShort(stream)
                .writeByte(SocketCode.REGISTER)
                .writeInt(body.readableBytes())
                .writeBytes(body)
                .asByteBuf();

        context.writeAndFlush(request.retain());
    }

    @Nonnull
    public Compression getCompression()
    {
        return compression;
    }

    protected void setupKeepAlive()
    {
        if (!connected) return;

        keepAliveThread = context.executor().scheduleWithFixedDelay(this::sendKeepAlive, 0, 5, TimeUnit.SECONDS);
    }

    protected void sendKeepAlive()
    {
        if (missedHeartbeats >= 2)
        {
            missedHeartbeats = 0;
            LOG.warn("Missed 2 heartbeats! Trying to reconnect...");
            close();
        }
        else
        {
            missedHeartbeats += 1;
            sendIdentify(byteBuf -> context.writeAndFlush(byteBuf.retain()));
            heartbeatStartTime = System.currentTimeMillis();
        }
    }

    public void shutdown()
    {
        boolean callOnShutdown = MiscUtil.locked(reconnectLock, () ->
        {
            if (shutdown)
                return false;
            shutdown = true;
            shouldReconnect = false;
            if (connectNode != null)
                api.getSessionController().removeSession(connectNode);
            boolean wasConnected = connected;
            close();
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
        return this.context;
    }

    private synchronized void sendIdentify(Consumer<? super ByteBuf> callback)
    {
        ByteBuf request = Unpooled.directBuffer()
                .writeByte(this.api.getVersion())
                .writeByte(DEFAULT_FLAG)
                .writeShort(DEFAULT_STREAM_ID)
                .writeByte(SocketCode.OPTIONS)
                .writeInt(0)
                .asByteBuf();

        context.writeAndFlush(request.retain());
    }

    public synchronized void connect()
    {
        if (this.api.getStatus() != Library.Status.ATTEMPTING_TO_RECONNECT)
        {
            this.api.setStatus(Library.Status.CONNECTING_TO_SOCKET);
        }

        initiating = true;

        ChannelFuture connect = bootstrap.connect(address).awaitUninterruptibly();

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
                throw new IllegalStateException(failure);
            }
        }

        LOG.error("There was an error in the Socket connection", failure);
        this.api.handleEvent(new ExceptionEvent(this.api, failure, true));
    }

    @Override
    public void close()
    {
        if (context != null)
            context.close();
    }

    public final void reconnect(boolean callFromQueue)
    {
        String message = "";
        if (callFromQueue)
            message = String.format("Queue is attempting to reconnect a shard...%s ", shardInfo != null ? " Shard: " + shardInfo.getShardString() : "");

        LOG.debug("{}Attempting to reconnect in {}s", message, reconnectTimeoutS);

        boolean isShutdown = MiscUtil.locked(reconnectLock, () ->
        {
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

                    identify = false;
                    api.setStatus(Library.Status.ATTEMPTING_TO_RECONNECT);
                    LOG.debug("Attempting to reconnect!");
                    connect();
                    break;
                }
                catch (RejectedExecutionException | InterruptedException failure)
                {
                    return true;
                }
                catch (IllegalStateException failure)
                {
                    LOG.debug("Encountered I/O error");
                    reconnect(false);
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

        if (keepAliveThread != null)
        {
            keepAliveThread.cancel(false);
            keepAliveThread = null;
        }

        if (!shouldReconnect || executor.isShutdown())
        {
            if (bootstrap != null)
                bootstrap = null;

            onShutdown();
        }
        else
        {
            api.handleEvent(new SessionDisconnectEvent(this.api, OffsetDateTime.now()));
            reconnect(false);
        }
    }

    public boolean isReady()
    {
        return !initiating;
    }

    public final void ready()
    {
        if (initiating)
        {
            initiating = false;
            processingReady = false;
            if (firstInit)
            {
                firstInit = false;
                LibraryImpl.LOG.info("Finished Loading!");
                api.handleEvent(new ReadyEvent(api));
            }
            else
            {
                LibraryImpl.LOG.info("Finished (Re)Loading!");
                api.handleEvent(new SessionRecreateEvent(api));
            }
        }
        else
        {
            LibraryImpl.LOG.debug("Successfully resumed Session!");
            api.handleEvent(new SessionResumeEvent(api));
        }
        api.setStatus(Library.Status.CONNECTED);
    }

    protected class StartingNode extends ConnectNode
    {
        @Override
        public void run(boolean isLast) throws InterruptedException
        {
            if (shutdown)
                return;
            setupSendingThread();
            connect();
            if (isLast) return;

            try
            {
                api.awaitReady();
            }
            catch (IllegalStateException failure)
            {
                close();
                LOG.debug("Shutdown while trying to login");
            }
        }
    }

    protected abstract class ConnectNode implements SessionController.SessionConnectNode
    {
        @Nonnull
        @Override
        public Library getLibrary()
        {
            return api;
        }

        @Nonnull
        @Override
        public Library.ShardInfo getShardInfo()
        {
            return api.getShardInfo();
        }
    }
}
