package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public interface ObjectData
{
    @Nonnull
    EnumSet<ObjectCreateAction.Field> getFields();

    @Nonnull
    ByteBuf applyData();
}
