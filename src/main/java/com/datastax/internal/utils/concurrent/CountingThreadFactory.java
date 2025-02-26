package com.datastax.internal.utils.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class CountingThreadFactory implements ThreadFactory
{
    private final Supplier<String> identifier;
    private final AtomicLong count = new AtomicLong(1);
    private final boolean daemon;

    public CountingThreadFactory(Supplier<String> identifier, String specifier)
    {
        this(identifier, specifier, true);
    }

    public CountingThreadFactory(Supplier<String> identifier, String specifier, boolean daemon)
    {
        this.identifier = () -> identifier.get() + " " + specifier;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r)
    {
        final Thread thread = new Thread(r, identifier.get() + "-Worker " + count.getAndIncrement());
        thread.setDaemon(daemon);
        return thread;
    }
}
