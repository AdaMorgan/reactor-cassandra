package com.datastax.internal;

import com.datastax.annotations.Nonnull;
import com.datastax.api.ObjectFactory;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.internal.request.Requester;
import com.datastax.internal.utils.config.ThreadingConfig;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class ObjectFactoryImpl implements ObjectFactory {
    protected final Session info;
    protected final Cluster cluster;
    protected final ThreadingConfig threadConfig;
    protected final Requester requester;

    public ObjectFactoryImpl(Cluster info, ThreadingConfig threadConfig) {
        this.cluster = info;
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
    public synchronized void shutdown()
    {
        this.cluster.close();
    }

    public List<Row> execute(String route)
    {
        return this.info.execute(route).all();
    }

    public void login()
    {
        try
        {
            this.cluster.connect();
        }
        catch (NoHostAvailableException e)
        {
            LoggerFactory.getLogger(this.getClass()).warn("The token has been invalidated and the ShardManager will shutdown!");
            this.shutdown();
        }
    }
}
