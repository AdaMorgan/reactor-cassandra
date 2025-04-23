package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.utils.request.ObjectCreateRequest;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.List;

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

    @Nonnull
    @Override
    default List<ByteBuf> getValues()
    {
        return getBuilder().getValues();
    }
}
