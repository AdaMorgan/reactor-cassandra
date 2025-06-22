package com.github.adamorgan.internal.utils.collections;

import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.api.utils.collections.TByteMap;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.UnlockHook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TByteHashMap<K, R> implements TByteMap<K, R>
{
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final Map<? extends K, ? extends R> values;

    public TByteHashMap(Map<? extends K, ? extends R> values)
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
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean containsKey(Object key)
    {
        Checks.notNull(key, "key");
        return true;
    }

    @Override
    public boolean containsValue(Object value)
    {
        return false;
    }

    @Override
    public R get(Object key)
    {
        return null;
    }

    @Nullable
    @Override
    public R put(K key, R value)
    {
        return null;
    }

    @Override
    public R remove(Object key)
    {
        return null;
    }

    @Override
    public void putAll(@Nonnull Map<? extends K, ? extends R> m)
    {

    }

    @Override
    public void clear()
    {

    }

    @Nonnull
    @Override
    public Set<K> keySet()
    {
        return Collections.emptySet();
    }

    @Nonnull
    @Override
    public Collection<R> values()
    {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Set<Entry<K, R>> entrySet()
    {
        return Collections.emptySet();
    }
}
