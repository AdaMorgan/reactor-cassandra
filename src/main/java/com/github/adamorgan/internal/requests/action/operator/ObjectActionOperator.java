package com.github.adamorgan.internal.requests.action.operator;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.internal.utils.request.ObjectData;

import javax.annotation.Nonnull;
import java.util.EnumSet;
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
    public ObjectAction<O> useTrace(boolean enable)
    {
        this.action.useTrace(enable);
        return this;
    }

    @Nonnull
    @Override
    public EnumSet<Flags> getFlags()
    {
        return action.getFlags();
    }

    @Override
    public int getRawFlags()
    {
        return action.getRawFlags();
    }

    @Nonnull
    @Override
    public ObjectAction<O> deadline(long timestamp)
    {
        this.action.deadline(timestamp);
        return this;
    }

    @Override
    public long getDeadline()
    {
        return action.getDeadline();
    }

    @Override
    public int getStreamId()
    {
        return this.action.getStreamId();
    }

    @Nonnull
    @Override
    public Library getLibrary()
    {
        return this.action.getLibrary();
    }

    @Nonnull
    @Override
    public ObjectData finalizeData()
    {
        return this.action.finalizeData();
    }
}
