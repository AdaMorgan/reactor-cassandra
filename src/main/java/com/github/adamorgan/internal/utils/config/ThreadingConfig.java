package com.github.adamorgan.internal.utils.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ThreadingConfig
{
    protected ExecutorService callbackPool;
    protected ExecutorService eventPool;
    protected EventLoopGroup eventLoopScheduler;

    protected boolean shutdownCallbackPool;
    protected boolean shutdownEventPool;
    protected boolean shutdownEventLoopScheduler;

    public ThreadingConfig()
    {
        this.shutdownEventLoopScheduler = true;
        this.shutdownCallbackPool = false;
    }

    public void setEventLoopScheduler(@Nullable EventLoopGroup executor, boolean shutdown)
    {
        this.eventLoopScheduler = executor == null ? new NioEventLoopGroup() : executor;
        this.shutdownEventLoopScheduler = shutdown;
    }

    public void setCallbackPool(@Nullable ExecutorService executor, boolean shutdown)
    {
        this.callbackPool = executor == null ? ForkJoinPool.commonPool() : executor;
        this.shutdownCallbackPool = shutdown;
    }

    public void setEventPool(@Nullable ExecutorService executor, boolean shutdown)
    {
        this.eventPool = executor;
        this.shutdownEventPool = shutdown;
    }

    public void shutdown()
    {
        if (shutdownEventLoopScheduler)
            eventLoopScheduler.shutdownGracefully();
        if (shutdownCallbackPool)
            callbackPool.shutdown();
        if (shutdownEventPool)
            eventPool.shutdownNow();
    }

    public void shutdownNow()
    {
        if (shutdownEventLoopScheduler)
            eventLoopScheduler.shutdownGracefully();
        if (shutdownCallbackPool)
            callbackPool.shutdownNow();
        if (shutdownEventPool)
            eventPool.shutdownNow();
    }

    @Nonnull
    public EventLoopGroup getEventLoopScheduler()
    {
        return eventLoopScheduler;
    }

    @Nonnull
    public ExecutorService getCallbackPool()
    {
        return callbackPool;
    }

    @Nullable
    public ExecutorService getEventPool()
    {
        return eventPool;
    }
}
