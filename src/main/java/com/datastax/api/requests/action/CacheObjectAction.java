package com.datastax.api.requests.action;

import com.datastax.api.requests.ObjectAction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public interface CacheObjectAction<T> extends ObjectAction<T>
{
    @Nonnull
    @CheckReturnValue
    CacheObjectAction<T> useCache(boolean useCache);
}
