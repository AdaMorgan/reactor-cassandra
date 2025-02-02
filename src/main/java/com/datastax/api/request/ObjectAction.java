package com.datastax.api.request;

import com.datastax.annotations.CheckReturnValue;
import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;
import com.datastax.api.request.objectaction.operator.FilterObjectAction;
import com.datastax.api.request.objectaction.operator.FlatMapObjectAction;
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

    default void queue()
    {
        queue(null);
    }

    default void queue(@Nullable Consumer<? super T> success)
    {
        queue(success, null);
    }

    void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure);

    @Nonnull
    @CheckReturnValue
    default <R> ObjectAction<R> map(@Nonnull Function<? super T, ? extends R> map)
    {
        Checks.notNull(map, "Function");
        return new MapObjectAction<>(this, map);
    }

    @Nonnull
    @CheckReturnValue
    default <O> ObjectAction<O> flatMap(@Nonnull Function<? super T, ? extends ObjectAction<O>> flatMap)
    {
        Checks.notNull(flatMap, "Function");
        return flatMap(null, flatMap);
    }

    @Nonnull
    @CheckReturnValue
    default <O> ObjectAction<O> flatMap(@Nullable Predicate<? super T> condition, @Nonnull Function<? super T, ? extends ObjectAction<O>> flatMap)
    {
        Checks.notNull(flatMap, "Function");
        return new FlatMapObjectAction<>(this, condition, flatMap);
    }

    @Nonnull
    @CheckReturnValue
    default ObjectAction<T> filter(@Nonnull Predicate<? super T> condition)
    {
        return new FilterObjectAction<>(this, condition);
    }

    default List<T> toList()
    {
        return null;
    }

    @Nonnull
    @CheckReturnValue
    CompletableFuture<T> submit();

    static boolean isPassContext()
    {
        return ObjectActionImpl.isPassContext();
    }
}
