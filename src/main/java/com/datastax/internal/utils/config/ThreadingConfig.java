package com.datastax.internal.utils.config;

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

    public static ThreadingConfig getDefault()
    {
        return new ThreadingConfig();
    }

    public void setCallbackPool(ExecutorService executor, boolean shutdown)
    {
        this.callbackPool = executor == null ? ForkJoinPool.commonPool() : executor;
        this.shutdownCallbackPool = shutdown;
    }

    public ExecutorService getCallbackPool()
    {
        return callbackPool;
    }
}
