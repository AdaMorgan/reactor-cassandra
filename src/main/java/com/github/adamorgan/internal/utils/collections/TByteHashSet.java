package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.api.utils.collections.TByteList;
import com.github.adamorgan.api.utils.collections.TByteSet;
import com.github.adamorgan.internal.utils.UnlockHook;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TByteHashSet<T> implements TByteSet<T>
{
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final Set<T> array;

    protected TByteHashSet(Collection<ByteBuf> values, Function<ByteBuf, ? extends T> decoder)
    {
        this.array = values.stream().map(decoder).collect(Collectors.toCollection(HashSet::new));
    }

    protected UnlockHook writeLock()
    {
        if (lock.getReadHoldCount() > 0)
            throw new IllegalStateException("Unable to acquire write-lock while holding read-lock!");
        Lock writeLock = lock.writeLock();
        MiscUtil.tryLock(writeLock);
        return new UnlockHook(writeLock);
    }

    protected UnlockHook readLock()
    {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        MiscUtil.tryLock(readLock);
        return new UnlockHook(readLock);
    }

    public List<T> asList()
    {
        try (UnlockHook hook = readLock())
        {
            return new ArrayList<>(this.array.isEmpty() ? Collections.emptyList() : this.array);
        }
    }

    @Nonnull
    @Override
    public Iterator<T> iterator()
    {
        try (UnlockHook unlock = readLock())
        {
            return array.iterator();
        }
    }

    @Override
    public void forEach(Consumer<? super T> action)
    {
        try (UnlockHook hook = readLock())
        {
            this.array.forEach(action);
        }
    }

    @Override
    public int hashCode()
    {
        try (UnlockHook hook = readLock())
        {
            return array.hashCode();
        }
    }

    @Override
    public String toString()
    {
        return asList().toString();
    }
}
