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

import com.github.adamorgan.api.utils.cache.CacheView;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.UnlockHook;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class AbstractCacheViewImpl<T> extends ReadWriteLockCache<T> implements CacheView<T>
{
    protected final TIntObjectMap<T> elements = new TIntObjectHashMap<>();
    protected final T[] emptyArray;

    public AbstractCacheViewImpl(Class<T> type)
    {
        this.emptyArray = ArrayUtils.newInstance(type, 0);
    }

    public TIntObjectMap<T> getMap()
    {
        if (!lock.writeLock().isHeldByCurrentThread())
            throw new IllegalStateException("Cannot access map directly without holding write lock!");
        return elements;
    }

    public T get(int hashCode)
    {
        try (UnlockHook hook = readLock())
        {
            return elements.get(hashCode);
        }
    }

    public T remove(int hashCode)
    {
        try (UnlockHook hook = writeLock())
        {
            return elements.remove(hashCode);
        }
    }

    public TIntHashSet keySet()
    {
        try (UnlockHook hook = readLock())
        {
            return new TIntHashSet(elements.keySet());
        }
    }

    @Nonnull
    @Override
    public List<T> asList()
    {
        try (UnlockHook hook = readLock())
        {
            return getCacheList() != null ? getCacheList() : cache(new ArrayList<>(elements.isEmpty() ? Collections.emptyList() : elements.valueCollection()));
        }
    }

    @Override
    public int size()
    {
        return elements.size();
    }

    @Override
    public boolean isEmpty()
    {
        return elements.isEmpty();
    }

    @Override
    public boolean containsKey(int key)
    {
        try (UnlockHook hook = readLock())
        {
            return this.elements.containsKey(key);
        }
    }

    @Override
    public boolean containsValue(@Nonnull T value)
    {
        try (UnlockHook hook = readLock())
        {
            return this.elements.containsValue(value);
        }
    }

    @Override
    public void clear()
    {
        try (UnlockHook hook = writeLock())
        {
            elements.clear();
        }
    }

    @Nonnull
    @Override
    public Iterator<T> iterator()
    {
        try (UnlockHook hook = readLock())
        {
            return new ObjectArrayIterator<>(elements.values(emptyArray));
        }
    }

    @Override
    public void forEach(Consumer<? super T> action)
    {
        Checks.notNull(action, "Consumer Action");
        try (UnlockHook hook = readLock())
        {
            elements.valueCollection().forEach(action);
        }
    }

    @Override
    public int hashCode()
    {
        try (UnlockHook hook = readLock())
        {
            return elements.hashCode();
        }
    }

    @Override
    public String toString()
    {
        return asList().toString();
    }
}
