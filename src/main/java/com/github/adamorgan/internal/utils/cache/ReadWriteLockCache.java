package com.github.adamorgan.internal.utils.cache;

import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.internal.utils.UnlockHook;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class ReadWriteLockCache<T>
{
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected WeakReference<List<T>> cacheList;

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

    protected List<T> getCacheList()
    {
        return cacheList != null ? cacheList.get() : null;
    }

    protected List<T> cache(List<T> list)
    {
        try (UnlockHook hook = writeLock())
        {
            list = Collections.unmodifiableList(list);
            this.cacheList = new WeakReference<>(list);
            return list;
        }
    }
}
