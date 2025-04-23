package com.github.adamorgan.api.requests.action;

import com.github.adamorgan.api.requests.ObjectAction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public interface CacheObjectAction<T> extends ObjectAction<T>
{
    @Nonnull
    @CheckReturnValue
    CacheObjectAction<T> useCache(boolean useCache);
}
