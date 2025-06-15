package com.github.adamorgan.internal.requests.action.operator;

import com.github.adamorgan.api.requests.ObjectAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Override
    public O complete(boolean shouldQueue)
    {
        return map.apply(action.complete(shouldQueue));
    }

    @Nonnull
    @Override
    public CompletableFuture<O> submit(boolean shouldQueue)
    {
        return action.submit(shouldQueue).thenApply(map);
    }
}
