package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.request.ObjectCreateRequest;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.EncodingUtils;
import com.github.adamorgan.internal.utils.Helpers;
import com.github.adamorgan.internal.utils.requestbody.BinaryType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collector;

public class ObjectCreateBuilder extends AbstractObjectBuilder<ObjectCreateBuilder> implements ObjectCreateRequest<ObjectCreateBuilder>
{
    @Nonnull
    @Override
    public <R extends Serializable> ObjectCreateBuilder setContent(@Nullable String content, @Nonnull Collection<? extends R> args)
    {
        Checks.notNull(args, "Values");
        Helpers.setContent(this.content, content);
        this.body.clear();
        if (!args.isEmpty())
        {
            this.fields |= ObjectCreateAction.Field.VALUES.getRawValue();
            ByteBuf body = args.stream().collect(Collector.of(Unpooled::directBuffer, BinaryType::pack0, ByteBuf::writeBytes));
            this.body.writeShort(args.size());
            this.body.writeBytes(body.retain());
        }
        return this;
    }

    @Nonnull
    @Override
    public <R extends Serializable> ObjectCreateBuilder setContent(@Nullable String content, @Nonnull Map<String, ? extends R> args)
    {
        Checks.notNull(args, "Values");
        Helpers.setContent(this.content, content);
        this.body.clear();
        if (!args.isEmpty())
        {
            this.fields |= ObjectCreateAction.Field.VALUES.getRawValue();
            ByteBuf body = args.entrySet().stream().collect(Collector.of(Unpooled::directBuffer, BinaryType::pack0, ByteBuf::writeBytes));
            this.body.writeShort(args.size());
            this.body.writeBytes(body.retain());
        }
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder addContent(@Nonnull String content)
    {
        Checks.notNull(content, "Content");
        this.content.append(content);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder setConsistency(ObjectCreateAction.Consistency consistency)
    {
        Checks.notNull(consistency, "Consistency");
        this.consistency = consistency;
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setMaxBufferSize(int bufferSize)
    {
        Checks.notNegative(bufferSize, "The buffer size");
        this.maxBufferSize = bufferSize;
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setNonce(long timestamp)
    {
        Checks.notNegative(timestamp, "Nonce");
        this.nonce = timestamp;
        return this;
    }
}
