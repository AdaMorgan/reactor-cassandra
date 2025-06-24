package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.api.utils.collections.TByteMap;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.UnlockHook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TByteHashMap<K, R> implements TByteMap<K, R>, Serializable
{
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final Map<K, R> values;

    public TByteHashMap(Map<K, R> values)
    {
        this.values = values;
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

    @Override
    public int size()
    {
        return this.values.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.values.isEmpty();
    }

    @Override
    public boolean containsKey(@Nonnull Object key)
    {
        Checks.notNull(key, "key");
        try (UnlockHook hook = readLock())
        {
            return this.values.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(Object value)
    {
        Checks.notNull(value, "value");
        try (UnlockHook hook = readLock())
        {
            return this.values.containsValue(value);
        }
    }

    @Override
    public R get(Object key)
    {
        Checks.notNull(key, "key");
        try (UnlockHook hook = readLock())
        {
            return this.values.get(key);
        }
    }

    @Nullable
    @Override
    public R put(K key, R value)
    {
        Checks.notNull(key, "key");
        try (UnlockHook hook = writeLock())
        {
            return this.values.put(key, value);
        }
    }

    @Override
    public R remove(Object key)
    {
        Checks.notNull(key, "key");
        try (UnlockHook hook = readLock())
        {
            return this.values.remove(key);
        }
    }

    @Override
    public void putAll(@Nonnull Map<? extends K, ? extends R> map)
    {
        Checks.notNull(map, "map");
        try (UnlockHook hook = writeLock())
        {
            this.values.putAll(map);
        }
    }

    @Override
    public void clear()
    {
        this.values.clear();
    }

    @Nonnull
    @Override
    public Set<K> keySet()
    {
        try (UnlockHook hook = readLock())
        {
            return Collections.unmodifiableSet(this.values.keySet());
        }
    }

    @Nonnull
    @Override
    public Collection<R> values()
    {
        try (UnlockHook hook = readLock())
        {
            return Collections.unmodifiableCollection(this.values.values());
        }
    }

    @Nonnull
    @Override
    public Set<Entry<K, R>> entrySet()
    {
        try (UnlockHook hook = readLock())
        {
            return Collections.unmodifiableSet(this.values.entrySet());
        }
    }
}
