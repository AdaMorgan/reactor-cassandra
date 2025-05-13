package com.github.adamorgan.api.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.requestbody.BinaryType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collector;

public interface ObjectCreateRequest<T extends ObjectCreateRequest<T>> extends ObjectRequest<T>
{
    @Nonnull
    T addContent(@Nonnull String content);

    @Nonnull
    default <R extends Serializable> T setContent(@Nonnull String content, @Nonnull Collection<? extends R> args)
    {
        Checks.notNull(content, "content");
        ByteBuf body = args.stream().collect(Collector.of(Unpooled::directBuffer, BinaryType::pack0, ByteBuf::writeBytes));
        return setContent(content, body, args.size(), false);
    }

    @Nonnull
    default <R extends Serializable> T setContent(@Nonnull String content, @Nonnull Map<String, ? extends R> args)
    {
        Checks.notNull(content, "content");
        ByteBuf body = args.entrySet().stream().collect(Collector.of(Unpooled::directBuffer, BinaryType::pack0, ByteBuf::writeBytes));
        return setContent(content, body, args.size(), true);
    }

    @Nonnull
    T setContent(@Nonnull String content, @Nonnull ByteBuf args, int size, boolean named);

    @Nonnull
    T setNonce(long timestamp);

    @Nonnull
    T setConsistency(ObjectCreateAction.Consistency consistency);
}
