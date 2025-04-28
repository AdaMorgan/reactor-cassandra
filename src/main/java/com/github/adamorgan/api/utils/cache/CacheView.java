package com.github.adamorgan.api.utils.cache;

import javax.annotation.Nonnull;
import java.util.List;

public interface CacheView<T> extends Iterable<T>
{
    @Nonnull
    List<T> asList();

    void clear();

    int size();

    boolean isEmpty();

    boolean containsKey(int key);

    boolean containsValue(@Nonnull T value);
}
