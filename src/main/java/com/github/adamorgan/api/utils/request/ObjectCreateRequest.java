package com.github.adamorgan.api.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface ObjectCreateRequest<T extends ObjectCreateRequest<T>> extends ObjectRequest<T>
{
    @Nonnull
    T addContent(@Nonnull String content);

    @Nonnull
    <R extends Serializable> T setContent(@Nullable String content, @Nonnull Collection<? extends R> args);

    @Nonnull
    <R extends Serializable> T setContent(@Nullable String content, @Nonnull Map<String, ? extends R> args);

    @Nonnull
    T setNonce(long timestamp);

    @Nonnull
    T setConsistency(ObjectCreateAction.Consistency consistency);
}
