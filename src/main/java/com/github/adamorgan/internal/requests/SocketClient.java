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
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SocketClient extends ByteToMessageCodec<ByteBuf>
{
    public static final Logger LOG = LibraryLogger.getLog(SocketClient.class);

    public static final byte DEFAULT_FLAG = 0x00;
    public static final int DEFAULT_STREAM_ID = 0x00;

    private final LibraryImpl api;
    private final Compression compression;

    private int reconnectTimeoutS = 2;

    public ChannelHandlerContext context;

    protected final StartingNode connectNode;
    protected final EventLoopGroup executor;
    protected final SessionController controller;

    protected final ReentrantLock reconnectLock = new ReentrantLock();
    protected final Condition reconnectCondvar = reconnectLock.newCondition();

    protected boolean initiating;

    protected volatile boolean shutdown = false;
    protected boolean shouldReconnect;
    protected boolean connected = false;

    protected boolean firstInit = true;
    protected boolean processingReady = true;

    private static final ThreadLocal<ByteBuf> CURRENT_EVENT = new ThreadLocal<>();
    private final SocketAddress address;

    public SocketClient(@Nonnull LibraryImpl api, SocketAddress address, Compression compression)
    {
        this.api = api;
        this.compression = compression;
        this.address = address;
        this.shouldReconnect = api.isAutoReconnect();
        this.executor = api.getCallbackPool();
        this.controller = api.getSessionController();
        this.connectNode = new StartingNode(this, controller::appendSession);
    }

    @Override
    public void channelActive(ChannelHandlerContext context)
    {
        this.context = context;
        sendIdentify(context, context::writeAndFlush);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context)
    {
        connected = false;
        handleDisconnect();
    }

    @Override
    protected void encode(ChannelHandlerContext context, ByteBuf message, ByteBuf out) throws Exception
    {
        out.writeBytes(message.retain());
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf input, List<Object> out)
    {
        input.markReaderIndex();

        if (input.readableBytes() < 9)
        {
            input.resetReaderIndex();
            return;
        }

        byte versionHeader = input.readByte();

        byte version = (byte) ((256 + versionHeader) & 0x7F);
        boolean isResponse = ((256 + versionHeader) & 0x80) != 0;

        byte flags = input.readByte();
        short stream = input.readShort();
        byte opcode = input.readByte();
        int length = input.readInt();

        if (input.readableBytes() < length)
        {
            input.resetReaderIndex();
            return;
        }

        ByteBuf body = input.readRetainedSlice(length).asReadOnly();

        boolean isCompressed = (flags & 0x01) != 0;

        onDispatch(context, version, flags, stream, opcode, length, isCompressed ? this.compression.unpack(body) : body, context::writeAndFlush);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable failure) throws Exception
    {
        failure.printStackTrace();
    }

    private void onDispatch(ChannelHandlerContext context, byte version, byte flags, int stream, byte opcode, int length, ByteBuf body, Consumer<? super ByteBuf> callback)
    {
        switch (opcode)
        {
            case SocketCode.SUPPORTED:
            {
                api.setStatus(Library.Status.IDENTIFYING_SESSION);
                sendStartup(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.AUTHENTICATE:
            {
                this.api.setStatus(Library.Status.LOGGING_IN);
                verifyToken(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.AUTH_SUCCESS:
            {
                this.api.setStatus(Library.Status.LOGIN_CONFIRMATION);
                registry(version, flags, stream, opcode, length, callback);
                break;
            }
            case SocketCode.READY:
            {
                this.api.setStatus(Library.Status.CONNECTED);
                this.api.getClient().ready();
                break;
            }
            case SocketCode.ERROR:
                ErrorResponse errorResponse = ErrorResponse.from(body);
                ErrorResponseException exception = ErrorResponseException.create(errorResponse, body);
                this.api.getRequester().handleResponse(context, flags, stream, opcode, length, exception, body);
                break;
            case SocketCode.RESULT:
            {
                this.api.getRequester().handleResponse(context, flags, stream, opcode, length, null, body);
                break;
            }
            default:
            {
                throw new UnsupportedOperationException("Unsupported opcode: " + opcode);
            }
        }
    }

    @Nonnull
    private SessionController.SessionConnectNode sendStartup(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.api, () ->
        {
            Map<String, String> map = new HashMap<>();

            map.put("CQL_VERSION", LibraryInfo.CQL_VERSION);
            map.put("DRIVER_VERSION", LibraryInfo.DRIVER_VERSION);
            map.put("DRIVER_NAME", LibraryInfo.DRIVER_NAME);
            map.put("THROW_ON_OVERLOAD", LibraryInfo.THROW_ON_OVERLOAD);

            if (!this.api.getCompression().equals(Compression.NONE))
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

            return Unpooled.buffer().writeByte(version)
                    .writeByte(SocketClient.DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.STARTUP)
                    .writeInt(body.readableBytes())
                    .writeBytes(body)
                    .asByteBuf();
        }, callback);
    }

    @Nonnull
    private SessionController.SessionConnectNode verifyToken(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.api, () ->
        {
            byte[] token = this.api.getToken();
            return Unpooled.buffer().writeByte(version)
                    .writeByte(SocketClient.DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.AUTH_RESPONSE)
                    .writeInt(token.length)
                    .writeBytes(token)
                    .asByteBuf();
        }, callback);
    }

    @Nonnull
    private SessionController.SessionConnectNode registry(byte version, byte flags, int stream, byte opcode, int length, Consumer<? super ByteBuf> callback)
    {
        return new SocketClient.ConnectNode(this.api, () ->
        {
            ByteBuf body = Stream.of("SCHEMA_CHANGE", "TOPOLOGY_CHANGE", "STATUS_CHANGE").collect(Unpooled::buffer, EncodingUtils::packUTF88, ByteBuf::writeBytes);

            return Unpooled.buffer().writeByte(version)
                    .writeByte(SocketClient.DEFAULT_FLAG)
                    .writeShort(stream)
                    .writeByte(SocketCode.REGISTER)
                    .writeInt(body.readableBytes())
                    .writeBytes(body)
                    .asByteBuf();
        }, callback);
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
            if (shutdown)
                return false;
            shutdown = true;
            shouldReconnect = false;
            if (connectNode != null)
                api.getSessionController().removeSession(connectNode);
            boolean wasConnected = connected;
            reconnectCondvar.signalAll();
            return !wasConnected;
        });

        if (callOnShutdown)
        {
            onShutdown();
        }
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

    private synchronized SessionController.SessionConnectNode sendIdentify(ChannelHandlerContext context, Consumer<? super ByteBuf> callback)
    {
        LOG.debug("Sending Identify node...");
        return new ConnectNode(this.api, () -> Unpooled.buffer()
                .writeByte(this.api.getVersion())
                .writeByte(DEFAULT_FLAG)
                .writeShort(DEFAULT_STREAM_ID)
                .writeByte(SocketCode.OPTIONS)
                .writeInt(0)
                .asByteBuf(), callback);
    }

    public synchronized void connect() throws IOException
    {
        if (this.api.getStatus() != Library.Status.ATTEMPTING_TO_RECONNECT)
        {
            this.api.setStatus(Library.Status.CONNECTING_TO_SOCKET);
        }

        initiating = true;

        ChannelFuture connect = connectNode.connect(address).awaitUninterruptibly();

        ChannelHandlerContext context;

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
                    {
                        break;
                    }

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
                    LOG.debug("Encountered I/O error");
                    reconnect();
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
        connected = false;
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

        public StartingNode(@Nonnull SocketClient client, Consumer<StartingNode> callback)
        {
            this.api = client.api;
            this.callback = callback;
            this.connectNode = new Bootstrap().group(executor)
                    .channel(ThreadingConfig.SOCKET_CHANNEL)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
                    .handler(client)
                    .validate();
        }


        @Nonnull
        @Override
        public Library getLibrary()
        {
            return api;
        }

        @Nonnull
        public ChannelFuture connect(SocketAddress address)
        {
            return this.connectNode.connect(address).addListener(future -> this.callback.accept(this));
        }
    }
}
