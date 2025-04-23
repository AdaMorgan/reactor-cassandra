package com.github.adamorgan.api.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public interface ObjectRequest<T extends ObjectRequest<T>>
{
    @Nonnull
    String getContent();

    byte getFieldsRaw();

    @Nonnull
    EnumSet<ObjectCreateAction.Fields> getFields();

    @Nonnull
    List<ByteBuf> getValues();

    @Nonnull
    default <R> T setContent(@Nullable String content, @Nullable R... args)
    {
        return this.setContent(content, args == null ? Collections.emptyList() : Arrays.asList(args));
    }

    @Nonnull
    <R> T setContent(@Nullable String content, @Nonnull Collection<? super R> args);

    @Nonnull
    <R> T setContent(@Nullable String content, @Nonnull Map<String, ? super R> args);
}
