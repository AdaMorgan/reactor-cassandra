package com.github.adamorgan.internal.utils.cache;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
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

public class AbstractCacheViewImpl<T extends ObjectCreateAction> extends ReadWriteLockCache<T> implements CacheView<T>
{
    protected final TIntObjectMap<T> elements;
    protected final T[] emptyArray;

    public AbstractCacheViewImpl(Class<T> type)
    {
        this.elements = new TIntObjectHashMap<>();
        this.emptyArray = ArrayUtils.newInstance(type, 0);
    }

    public T remove(int index)
    {
        try (UnlockHook hook = writeLock())
        {
            return elements.remove(index);
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
