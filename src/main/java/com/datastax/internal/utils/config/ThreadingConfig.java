package com.datastax.internal.utils.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ThreadingConfig
{
    private final ExecutorService callbackPool;
    private final ExecutorService eventPool;

    private final boolean shutdownCallbackPool;
    private final boolean shutdownEventPool;

    public ThreadingConfig()
    {
        this(null);
    }

    public ThreadingConfig(@Nullable ExecutorService callbackPool)
    {
        this(callbackPool,null);
    }

    public ThreadingConfig(@Nullable ExecutorService callbackPool, @Nullable ExecutorService eventPool)
    {
        this(callbackPool, eventPool,true);
    }

    public ThreadingConfig(@Nullable ExecutorService callbackPool, @Nullable ExecutorService eventPool, boolean shutdown)
    {
        this.callbackPool = callbackPool == null ? ForkJoinPool.commonPool() : callbackPool;
        this.eventPool = eventPool;
        this.shutdownCallbackPool = shutdown;
        this.shutdownEventPool = eventPool != null;
    }

    public void shutdown()
    {
        if (shutdownCallbackPool)
            callbackPool.shutdown();
        if (shutdownEventPool)
            eventPool.shutdown();
    }

    public void shutdownNow()
    {
        if (shutdownCallbackPool)
            callbackPool.shutdownNow();
        if (shutdownEventPool)
            eventPool.shutdownNow();
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
