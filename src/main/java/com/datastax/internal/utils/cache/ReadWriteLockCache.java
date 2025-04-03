package com.datastax.internal.utils.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class ReadWriteLockCache<T> extends LinkedList<T>
{

    public ReadWriteLockCache(int initialCapacity)
    {
        super(Collections.unmodifiableList(new ArrayList<>(initialCapacity)));
    }
}
