package com.datastax.internal.requests.action.operator;

import com.datastax.api.requests.ObjectAction;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public abstract class ObjectActionOperator<I, O> implements ObjectAction<O>
{
    protected final ObjectAction<I> action;

    protected ObjectActionOperator(ObjectAction<I> action)
    {
        this.action = action;
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        return this.action.applyData();
    }
}
