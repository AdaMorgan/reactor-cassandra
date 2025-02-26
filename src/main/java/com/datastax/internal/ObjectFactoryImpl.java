package com.datastax.internal;

import com.datastax.api.ObjectFactory;
import com.datastax.internal.request.Requester;
import com.datastax.internal.utils.config.ThreadingConfig;

import java.util.concurrent.ExecutorService;

public class ObjectFactoryImpl implements ObjectFactory
{
    protected final ThreadingConfig threadConfig;
    protected final Requester requester;

    public ObjectFactoryImpl(ThreadingConfig threadConfig)
    {
        this.threadConfig = threadConfig;
        this.requester = new Requester(this);
    }

    @Override
    public ExecutorService getCallbackPool()
    {
        return threadConfig.getCallbackPool();
    }

    public Requester getRequester() {
        return requester;
    }

    @Override
    public synchronized void shutdown()
    {

    }

    public void login()
    {

    }
}
