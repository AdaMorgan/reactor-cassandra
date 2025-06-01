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
        this.callbackPool = executor == null ? new MultiThreadIoEventLoopGroup(200, handler) : new MultiThreadIoEventLoopGroup(executor, handler);
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
        if (shutdownEventPool)
            eventPool.shutdownNow();
    }

    public void shutdownNow()
    {
        if (shutdownCallbackPool)
            callbackPool.shutdownGracefully();
        if (shutdownEventPool)
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
