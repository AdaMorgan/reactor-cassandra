package com.datastax.api.request;

import com.datastax.annotations.CheckReturnValue;
import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;
import com.datastax.api.request.objectaction.operator.FilterObjectAction;
import com.datastax.api.request.objectaction.operator.MapObjectAction;
import com.datastax.internal.objectaction.ObjectActionImpl;
import com.datastax.internal.utils.Checks;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ObjectAction<T>
{
    @Nonnull
    static Consumer<Object> getDefaultSuccess()
    {
        return ObjectActionImpl.getDefaultSuccess();
    }

    @Nonnull
    static Consumer<? super Throwable> getDefaultFailure()
    {
        return ObjectActionImpl.getDefaultFailure();
    }

    @Nonnull
    @CheckReturnValue
    default <R> ObjectAction<R> map(@Nonnull Function<? super T, ? extends R> map)
    {
        Checks.notNull(map, "Function");
        return new MapObjectAction<>(this, map);
    }

    default void queue()
    {
        queue(null);
    }

    default void queue(@Nullable Consumer<? super T> success)
    {
        queue(success, null);
    }

    default ObjectAction<T> filter(@Nonnull Predicate<T> predicate)
    {
        Checks.notNull(predicate, "Predicate");
        return new FilterObjectAction<>(this, predicate);
    }

    default List<T> toList()
    {
        return null;
    }

    void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure);

    CompletableFuture<T> submit();

    static boolean isPassContext()
    {
        return ObjectActionImpl.isPassContext();
    }
}
