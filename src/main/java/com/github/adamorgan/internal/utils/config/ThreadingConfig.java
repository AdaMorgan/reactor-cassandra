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
import io.netty.channel.nio.NioIoHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;

public class ThreadingConfig
{
    protected final IoHandlerFactory handler;

    protected EventLoopGroup callbackPool;
    protected ExecutorService eventPool;

    protected boolean shutdownCallbackPool;
    protected boolean shutdownEventPool;

    public ThreadingConfig()
    {
        this.handler = Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory();
        this.shutdownCallbackPool = false;
    }

    public void setCallbackPool(@Nullable ExecutorService executor, boolean shutdown)
    {
        this.callbackPool = executor == null ? new MultiThreadIoEventLoopGroup(handler) : new MultiThreadIoEventLoopGroup(executor, handler);
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
