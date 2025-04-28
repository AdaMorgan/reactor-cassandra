package com.github.adamorgan.api.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public interface ObjectRequest<T extends ObjectRequest<T>>
{
    @Nonnull
    String getContent();

    int getFieldsRaw();

    @Nonnull
    EnumSet<ObjectCreateAction.Field> getFields();

    @Nonnull
    List<ByteBuf> getValues();

    int getMaxBufferSize();

    @Nonnull
    <R> T setContent(@Nullable String content, @Nonnull Collection<? super R> args);

    @Nonnull
    <R> T setContent(@Nullable String content, @Nonnull Map<String, ? super R> args);
}
