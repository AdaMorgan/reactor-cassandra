package com.datastax.api.request.objectaction.operator;

import com.datastax.api.request.ObjectAction;
import com.datastax.internal.objectaction.operator.ObjectActionOperator;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FilterObjectAction<I> extends ObjectActionOperator<I, I>
{
    protected final Predicate<? super I> condition;

    public FilterObjectAction(ObjectAction<I> action, Predicate<? super I> condition)
    {
        super(action);
        this.condition = condition;
    }

    @Override
    public void queue(Consumer<? super I> success, Consumer<? super Throwable> failure)
    {
        Consumer<? super Throwable> error = this.contextWrap(failure);
        handle(action, error, result -> {
            if (condition == null || !condition.test(result))
                return;

            doSuccess(success, result);
        });
    }

    @Override
    public CompletableFuture<I> submit(boolean shouldQueue)
    {
        CompletableFuture<I> future = new CompletableFuture<>();
        CompletableFuture<I> handle = this.action.submit();

        Consumer<? super Throwable> onFailure = contextWrap(future::completeExceptionally);

        handle.whenComplete((success, failure) -> {
            if (failure != null)
                onFailure.accept(failure);

            if (condition == null || !condition.test(success))
                future.complete(null);

            future.complete(success);
        });

        future.whenComplete((result, failure) -> {
           if (future.isCancelled())
               handle.cancel(false);
        });

        return future;
    }
}
