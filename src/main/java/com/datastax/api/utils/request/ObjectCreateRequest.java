package com.datastax.api.utils.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface ObjectCreateRequest<T extends ObjectCreateRequest<T>> extends ObjectRequest<T>
{
    @Nonnull
    T addContent(@Nonnull String content);

    @Nonnull
    <R> T addValues(@Nonnull Map<String, ? super R> values);

    @Nonnull
    <R> T addValues(@Nonnull Collection<? super R> values);

    @Nonnull
    default <R> T addValues(@Nullable R... values)
    {
        return addValues(values == null ? Collections.emptyList() : Arrays.asList(values));
    }
}
