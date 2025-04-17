package com.datastax.internal.requests.action.operator;

import com.datastax.api.requests.ObjectAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapObjectAction<I, O> extends ObjectActionOperator<I, O>
{
    protected final Function<? super I, ? extends O> map;

    public MapObjectAction(ObjectAction<I> action, Function<? super I, ? extends O> map)
    {
        super(action);
        this.map = map;
    }

    @Override
    public void queue(@Nullable Consumer<? super O> success, @Nullable Consumer<? super Throwable> failure)
    {
        handle(action, failure, (result) -> doSuccess(success, map.apply(result)));
    }

    @Nonnull
    @Override
    public CompletableFuture<O> submit(boolean shouldQueue)
    {
        return action.submit(shouldQueue).thenApply(map);
    }
}
