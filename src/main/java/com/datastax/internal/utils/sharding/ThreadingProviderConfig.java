package com.datastax.internal.utils.sharding;

import com.datastax.annotations.Nullable;
import com.datastax.api.sharding.ThreadPoolProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class ThreadingProviderConfig
{
    private final ThreadFactory factory;
    private final ThreadPoolProvider<? extends ExecutorService> callbackPoolProvider;

    public ThreadingProviderConfig(ThreadFactory factory, ThreadPoolProvider<? extends ExecutorService> callbackPoolProvider)
    {
        this.factory = factory;
        this.callbackPoolProvider = callbackPoolProvider;
    }

    private void shutdown(ThreadPoolProvider<?> provider)
    {
        if (provider instanceof ThreadPoolProvider.LazySharedProvider)
            ((ThreadPoolProvider.LazySharedProvider<?>) provider).shutdown();
    }

    @Nullable
    public ThreadFactory getThreadFactory()
    {
        return factory;
    }

    @Nullable
    public ThreadPoolProvider<? extends ExecutorService> getCallbackPoolProvider()
    {
        return callbackPoolProvider;
    }

    public void shutdown()
    {
        shutdown(callbackPoolProvider);
    }
}
