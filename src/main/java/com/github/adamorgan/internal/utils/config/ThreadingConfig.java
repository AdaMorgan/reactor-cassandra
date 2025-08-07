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

package com.github.adamorgan.internal.utils.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

public class ThreadingConfig
{
    protected EventLoopGroup callbackPool;
    protected ExecutorService eventPool;

    protected boolean shutdownCallbackPool;
    protected boolean shutdownEventPool;

    public final static int EPOLL = 1 << 1;
    public final static int KQUEUE = 1 << 2;
    public final static int IO_URING = 1 << 3;

    public static final int THREAD_CODE = (Epoll.isAvailable() ? EPOLL : 0) | (KQueue.isAvailable() ? KQUEUE : 0) | (IoUring.isAvailable() ? IO_URING : 0);

    private static final Predicate<Integer> HAS_FLAG = code -> (THREAD_CODE & code) != 0;

    public static final Class<? extends SocketChannel> SOCKET_CHANNEL = HAS_FLAG.test(IO_URING) ? IoUringSocketChannel.class : HAS_FLAG.test(EPOLL) ? EpollSocketChannel.class : HAS_FLAG.test(KQUEUE) ? KQueueSocketChannel.class : NioSocketChannel.class;;
    public static final IoHandlerFactory IO_HANDLER = HAS_FLAG.test(IO_URING) ? IoUringIoHandler.newFactory() : HAS_FLAG.test(EPOLL) ? EpollIoHandler.newFactory() : HAS_FLAG.test(KQUEUE) ? KQueueIoHandler.newFactory() : NioIoHandler.newFactory();


    public ThreadingConfig()
    {
        this.shutdownCallbackPool = false;
    }

    public void setCallbackPool(@Nullable ExecutorService executor, boolean shutdown)
    {
        this.callbackPool = executor == null ? new MultiThreadIoEventLoopGroup(IO_HANDLER) : new MultiThreadIoEventLoopGroup(executor, IO_HANDLER);
        this.shutdownCallbackPool = shutdown;
    }

    public void setEventPool(@Nullable ExecutorService executor, boolean shutdown)
    {
        this.eventPool = executor;
        this.shutdownEventPool = shutdown;
    }

    public boolean isAvailable()
    {
        return Epoll.isAvailable();
    }

    public void shutdown()
    {
        if (shutdownCallbackPool)
            callbackPool.shutdownGracefully();
        if (shutdownEventPool && eventPool != null)
            eventPool.shutdownNow();
    }

    public void shutdownNow()
    {
        if (shutdownCallbackPool)
            callbackPool.shutdownGracefully();
        if (shutdownEventPool || eventPool != null)
            eventPool.shutdownNow();
    }

    @Nonnull
    public EventLoopGroup getCallbackPool()
    {
        return callbackPool;
    }

    @Nullable
    public ExecutorService getEventPool()
    {
        return eventPool;
    }
}
