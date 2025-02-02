package com.datastax.api.request.objectaction.operator;

import com.datastax.annotations.Nullable;
import com.datastax.api.request.ObjectAction;
import com.datastax.internal.objectaction.operator.ObjectActionOperator;
import com.datastax.internal.utils.Checks;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FlatMapObjectAction<I, O> extends ObjectActionOperator<I, O>
{
    protected final Function<? super I, ? extends ObjectAction<O>> function;
    protected final Predicate<? super I> condition;

    public FlatMapObjectAction(ObjectAction<I> action, Predicate<? super I> condition, Function<? super I, ? extends ObjectAction<O>> function)
    {
        super(action);
        this.condition = condition;
        this.function = function;
    }

    private ObjectAction<O> supply(I input)
    {
        return this.function.apply(input);
    }

    @Override
    public void queue(@Nullable Consumer<? super O> success, @Nullable Consumer<? super Throwable> failure)
    {
        Consumer<? super Throwable> catcher = contextWrap(failure);
        handle(action, catcher, (result) -> {
            if (condition != null && !condition.test(result))
                return;

            ObjectAction<O> then = supply(result);
            Checks.notNull(then, "FlatMap");
            then.queue(success, failure);
        });
    }

    @Override
    public CompletableFuture<O> submit()
    {
        return this.action.submit().thenCompose((result) ->
        {
            if (condition != null && ! condition.test(result))
            {
                CompletableFuture<O> future = new CompletableFuture<>();
                future.cancel(true);
                return future;
            }

            return supply(result).submit();
        });
    }
}
