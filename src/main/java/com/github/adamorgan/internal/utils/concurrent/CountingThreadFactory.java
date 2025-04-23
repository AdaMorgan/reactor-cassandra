package com.github.adamorgan.internal.utils.concurrent;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class CountingThreadFactory implements ThreadFactory
{
    private final Supplier<String> identifier;
    private final AtomicLong count = new AtomicLong(1);
    private final boolean daemon;

    public CountingThreadFactory(@Nonnull Supplier<String> identifier, @Nonnull String specifier)
    {
        this(identifier, specifier, true);
    }

    public CountingThreadFactory(@Nonnull Supplier<String> identifier, @Nonnull String specifier, boolean daemon)
    {
        this.identifier = () -> identifier.get() + " " + specifier;
        this.daemon = daemon;
    }

    @Nonnull
    @Override
    public Thread newThread(@Nonnull Runnable r)
    {
        final Thread thread = new Thread(r, identifier.get() + "-Worker " + (count.getAndIncrement() - 1));
        thread.setDaemon(daemon);
        return thread;
    }
}
