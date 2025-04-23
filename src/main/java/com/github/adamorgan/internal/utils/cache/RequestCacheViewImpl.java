package com.github.adamorgan.internal.utils.cache;

import java.util.function.Function;

public class RequestCacheViewImpl<T> extends ReadWriteLockCache<T>
{
    public RequestCacheViewImpl(int initialCapacity)
    {
        super(initialCapacity);
    }

    public void cache(T element, Function<T, T> condition)
    {
        this.addLast(condition.apply(element));
    }
}
