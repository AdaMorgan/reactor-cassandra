package com.datastax.api.utils.request;

import javax.annotation.Nonnull;

public interface ObjectRequest<T extends ObjectRequest<T>>
{
    @Nonnull
    String getContent();
}
