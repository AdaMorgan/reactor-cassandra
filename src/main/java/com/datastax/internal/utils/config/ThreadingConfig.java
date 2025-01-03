package com.datastax.internal.utils.config;

import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ThreadingConfig {
    private ExecutorService callbackPool;

    private boolean shutdownCallbackPool;

    public ThreadingConfig()
    {
        this.callbackPool = ForkJoinPool.commonPool();
        this.shutdownCallbackPool = false;
    }

    @Nonnull
    public static ThreadingConfig getDefault()
    {
        return new ThreadingConfig();
    }

    public void setCallbackPool(@Nullable ExecutorService executor, boolean shutdown)
    {
        this.callbackPool = executor == null ? ForkJoinPool.commonPool() : executor;
        this.shutdownCallbackPool = shutdown;
    }

    @Nonnull
    public ExecutorService getCallbackPool()
    {
        return callbackPool;
    }
}
