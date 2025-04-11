package com.datastax.internal.utils.request;

import com.datastax.api.utils.request.ObjectCreateRequest;

import javax.annotation.Nonnull;

public interface AbstractObjectBuilderMixin<T extends ObjectCreateRequest<T>, R extends AbstractObjectBuilder<R>> extends ObjectCreateRequest<T>
{
    @Nonnull
    R getBuilder();

    @Nonnull
    @Override
    default String getContent()
    {
        return getBuilder().getContent();
    }
}
