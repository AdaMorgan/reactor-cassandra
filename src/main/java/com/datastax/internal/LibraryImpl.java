package com.datastax.internal;

import com.datastax.api.Library;
import com.datastax.internal.utils.CustomLogger;
import com.datastax.internal.utils.config.ThreadingConfig;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class LibraryImpl implements Library
{
    public static final Logger LOG = CustomLogger.getLog(Library.class);

    protected final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZING);
    protected final AtomicInteger responseTotal = new AtomicInteger(0);

    protected final byte[] token;

    protected final ThreadingConfig threadingConfig;

    public LibraryImpl(byte[] token, ThreadingConfig threadingConfig)
    {
        this.token = token;
        this.threadingConfig = threadingConfig;
    }

    @Nonnull
    @Override
    public byte[] getToken()
    {
        return token;
    }

    @Nonnull
    @Override
    public Status getStatus()
    {
        return status.get();
    }

    @Override
    public long getResponseTotal()
    {
        return responseTotal.get();
    }

    @Nonnull
    @Override
    public ExecutorService getCallbackPool()
    {
        return threadingConfig.getCallbackPool();
    }
}
