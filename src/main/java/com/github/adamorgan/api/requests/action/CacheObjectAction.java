package com.github.adamorgan.api.requests.action;

import com.github.adamorgan.api.requests.ObjectAction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public interface CacheObjectAction<T> extends ObjectAction<T>
{
    /**
     * Sets whether this request should rely on cached entities, or always retrieve a new one.
     *
     * @param  useCache
     *         True if the cache should be used when available, even if the entity might be outdated.
     *         False, to always request a new instance from the CQL Binary Protocol.
     *
     * @return This ObjectAction instance
     */
    @Nonnull
    @CheckReturnValue
    CacheObjectAction<T> useCache(boolean useCache);
}
