/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.internal.utils.cache;

import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.internal.utils.UnlockHook;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class ReadWriteLockCache<T>
{
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected WeakReference<List<T>> cacheList;

    public UnlockHook writeLock()
    {
        if (lock.getReadHoldCount() > 0)
            throw new IllegalStateException("Unable to acquire write-lock while holding read-lock!");
        Lock writeLock = lock.writeLock();
        MiscUtil.tryLock(writeLock);
        return new UnlockHook(writeLock);
    }

    public UnlockHook readLock()
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
