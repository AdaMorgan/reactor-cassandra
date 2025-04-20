package com.datastax.api.utils.request;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.List;

public interface ObjectRequest<T extends ObjectRequest<T>>
{
    @Nonnull
    String getContent();

    @Nonnull
    List<ByteBuf> getValues();
}
