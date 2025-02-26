package com.datastax.api.request;

import com.datastax.annotations.CheckReturnValue;
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
    static Consumer<Object> getDefaultSuccess()
    {
        return ObjectActionImpl.getDefaultSuccess();
    }

    static Consumer<? super Throwable> getDefaultFailure()
    {
        return ObjectActionImpl.getDefaultFailure();
    }

    default void queue()
    {
        queue(null);
    }

    default void queue(Consumer<? super T> success)
    {
        queue(success, null);
    }

    void queue(Consumer<? super T> success, Consumer<? super Throwable> failure);

    @CheckReturnValue
    default <R> ObjectAction<R> map(Function<? super T, ? extends R> map)
    {
        Checks.notNull(map, "Function");
        return new MapObjectAction<>(this, map);
    }

    @CheckReturnValue
    default <O> ObjectAction<O> flatMap(Function<? super T, ? extends ObjectAction<O>> flatMap)
    {
        Checks.notNull(flatMap, "Function");
        return flatMap(null, flatMap);
    }

    @CheckReturnValue
    default <O> ObjectAction<O> flatMap(Predicate<? super T> condition, Function<? super T, ? extends ObjectAction<O>> flatMap)
    {
        Checks.notNull(flatMap, "Function");
        return new FlatMapObjectAction<>(this, condition, flatMap);
    }

    @CheckReturnValue
    default ObjectAction<T> filter(Predicate<? super T> condition)
    {
        return new FilterObjectAction<>(this, condition);
    }

    default List<T> toList()
    {
        return null;
    }

    @CheckReturnValue
    default CompletableFuture<T> submit()
    {
        return submit(true);
    }

    @CheckReturnValue
    CompletableFuture<T> submit(boolean shouldQueue);

    boolean isPassContext();
}
