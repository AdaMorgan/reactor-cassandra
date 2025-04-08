package com.datastax.internal.utils.request;

import com.datastax.api.utils.request.ObjectRequest;

import javax.annotation.Nonnull;

public abstract class AbstractObjectBuilder<T extends AbstractObjectBuilder<T>> implements ObjectRequest<T>
{
    protected final StringBuilder content = new StringBuilder();

    @Nonnull
    @Override
    public String getContent()
    {
        return content.toString();
    }
}
