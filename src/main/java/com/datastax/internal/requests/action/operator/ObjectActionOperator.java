package com.datastax.internal.requests.action.operator;

import com.datastax.api.Library;
import com.datastax.api.requests.ObjectAction;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public abstract class ObjectActionOperator<I, O> implements ObjectAction<O>
{
    protected final ObjectAction<I> action;

    protected ObjectActionOperator(ObjectAction<I> action)
    {
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
        action.queue(result -> {
            try
            {
                if (success != null)
                    success.accept(result);
            }
            catch (Throwable ex)
            {
                doFailure(failure, ex);
            }
        }, failure);
    }

    @Nonnull
    @Override
    public Library getLibrary()
    {
        return this.action.getLibrary();
    }

    @Nonnull
    @Override
    public ByteBuf asByteBuf()
    {
        return this.action.asByteBuf();
    }
}
