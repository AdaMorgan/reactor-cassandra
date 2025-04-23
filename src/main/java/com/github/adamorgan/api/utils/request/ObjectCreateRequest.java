package com.github.adamorgan.api.utils.request;

import javax.annotation.Nonnull;

public interface ObjectCreateRequest<T extends ObjectCreateRequest<T>> extends ObjectRequest<T>
{
    @Nonnull
    T addContent(@Nonnull String content);
}
