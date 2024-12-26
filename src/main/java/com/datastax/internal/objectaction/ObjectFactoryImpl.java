package com.datastax.internal.objectaction;

import com.datastax.annotations.Nonnull;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.internal.utils.config.ThreadingConfig;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class ObjectFactoryImpl implements ObjectFactory {
    protected final Session info;
    protected final ThreadingConfig threadConfig;
    protected final Requester requester;

    public ObjectFactoryImpl(Cluster info, ThreadingConfig threadConfig) {
        this.info = info.newSession();
        this.threadConfig = threadConfig;
        this.requester = new Requester(this);
    }

    @Nonnull
    @Override
    public ExecutorService getCallbackPool()
    {
        return threadConfig.getCallbackPool();
    }

    @Nonnull
    public Requester getRequester() {
        return requester;
    }

    @Override
    public void shutdown()
    {

    }

    public List<Row> execute(String route)
    {
        return this.info.execute(route).all();
    }
}
