package com.datastax.internal;

import com.datastax.annotations.Nonnull;
import com.datastax.api.ObjectFactory;
import com.datastax.api.sharding.ObjectManager;
import com.datastax.internal.request.Requester;
import com.datastax.internal.utils.config.ThreadingConfig;

import java.util.concurrent.ExecutorService;

public class ObjectFactoryImpl implements ObjectFactory
{
    protected final ThreadingConfig threadConfig;
    protected final Requester requester;

    protected ObjectFactory.ShardInfo shardInfo;

    protected ObjectManager objectManager = null;

    public ObjectFactoryImpl(ThreadingConfig threadConfig)
    {
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
    @Override
    public ShardInfo getShardInfo() {
        return shardInfo == null ? ShardInfo.SINGLE : shardInfo;
    }

    @Nonnull
    public Requester getRequester() {
        return requester;
    }

    @Override
    public synchronized void shutdown()
    {

    }

    public void setShardManager(ObjectManager objectManager)
    {
        this.objectManager = objectManager;
    }

    public void login(ShardInfo shardInfo)
    {
        this.shardInfo = shardInfo;
    }
}
