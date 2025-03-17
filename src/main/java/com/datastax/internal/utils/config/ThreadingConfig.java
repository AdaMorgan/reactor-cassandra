package com.datastax.internal.utils.config;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ThreadingConfig
{
    private final ExecutorService callbackPool;
    private final boolean shutdownCallbackPool;

    public ThreadingConfig()
    {
        this(null);
    }

    public ThreadingConfig(@Nullable ExecutorService callbackPool)
    {
        this(callbackPool, true);
    }

    public ThreadingConfig(@Nullable ExecutorService callbackPool, boolean shutdown)
    {
        this.callbackPool = callbackPool == null ? ForkJoinPool.commonPool() : callbackPool;
        this.shutdownCallbackPool = shutdown;
    }

    public void shutdown()
    {
        if (shutdownCallbackPool)
            callbackPool.shutdown();
    }

    public void shutdownNow()
    {
        if (shutdownCallbackPool)
            callbackPool.shutdownNow();
    }

    public ExecutorService getCallbackPool()
    {
        return callbackPool;
    }
}
