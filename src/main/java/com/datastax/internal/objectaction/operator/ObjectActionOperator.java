package com.datastax.internal.objectaction.operator;

import com.datastax.api.exceptions.ContextException;
import com.datastax.api.request.ObjectAction;
import com.datastax.internal.objectaction.ObjectActionImpl;

import java.util.function.Consumer;

public abstract class ObjectActionOperator<I, O> implements ObjectAction<O> {
    protected final ObjectAction<I> action;

    public ObjectActionOperator(ObjectAction<I> action) {
        this.action = action;
    }

    protected static <E> void doSuccess(Consumer<? super E> callback, E value)
    {
        if (callback == null)
            ObjectAction.getDefaultSuccess().accept(value);
        else
            callback.accept(value);
    }

    protected static void doFailure(Consumer<? super Throwable> callback, Throwable throwable)
    {
        if (callback == null)
            ObjectAction.getDefaultFailure().accept(throwable);
        else
            callback.accept(throwable);
        if (throwable instanceof Error)
            throw (Error) throwable;
    }

    protected void handle(ObjectAction<I> action, Consumer<? super Throwable> failure, Consumer<? super I> success)
    {
        Consumer<? super Throwable> catcher = contextWrap(failure);
        action.queue((result) -> {
            try
            {
                if (success != null)
                    success.accept(result);
            }
            catch (Throwable ex)
            {
                doFailure(catcher, ex);
            }
        }, catcher);
    }

    protected Consumer<? super Throwable> contextWrap(Consumer<? super Throwable> callback)
    {
        if (callback instanceof ContextException.ContextConsumer)
            return callback;
        else if (this.action.isPassContext())
            return ContextException.here(callback == null ? ObjectAction.getDefaultFailure() : callback);
        return callback;
    }

    @Override
    public boolean isPassContext()
    {
        return this.action.isPassContext();
    }
}
