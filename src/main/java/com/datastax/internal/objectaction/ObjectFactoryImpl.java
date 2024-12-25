package com.datastax.internal.objectaction;

import com.datastax.annotations.Nonnull;
import com.datastax.driver.core.Cluster;
import com.datastax.internal.utils.config.ThreadingConfig;

import java.util.concurrent.ExecutorService;

public class ObjectFactoryImpl implements ObjectFactory {
    protected final Cluster cluster;
    protected final ThreadingConfig threadConfig;

    public ObjectFactoryImpl(Cluster cluster, ThreadingConfig threadConfig) {
        this.cluster = cluster;
        this.threadConfig = threadConfig;
    }

    @Nonnull
    @Override
    public ExecutorService getCallbackPool()
    {
        return threadConfig.getCallbackPool();
    }

    @Override
    public void shutdown() {

    }
}
