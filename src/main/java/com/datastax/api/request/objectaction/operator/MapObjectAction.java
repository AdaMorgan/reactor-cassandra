package com.datastax.api.request.objectaction.operator;

import com.datastax.api.request.ObjectAction;
import com.datastax.internal.objectaction.operator.ObjectActionOperator;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapObjectAction<I, O> extends ObjectActionOperator<I, O>
{
    private final Function<? super I, ? extends O> function;

    public MapObjectAction(ObjectAction<I> action, Function<? super I, ? extends O> function)
    {
        super(action);
        this.function = function;
    }

    @Override
    public void queue(Consumer<? super O> success, Consumer<? super Throwable> failure)
    {
        handle(action, failure, (result) -> doSuccess(success, function.apply(result)));
    }

    @Override
    public CompletableFuture<O> submit(boolean shouldQueue) {
        return action.submit().thenApply(function);
    }
}
