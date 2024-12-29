package com.datastax.api.request;

import com.datastax.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ObjectAction<T>
{
    String getRoute();

    default void queue()
    {
        queue(null);
    }

    default void queue(@Nullable Consumer<? super T> success)
    {
        queue(success, null);
    }

    void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure);

    CompletableFuture<T> submit();
}
