package com.github.adamorgan.api.utils.cache;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public interface CacheView<T> extends Iterable<T>
{
    @Nonnull
    List<T> asList();

    int size();

    boolean isEmpty();

    void clear();
}
