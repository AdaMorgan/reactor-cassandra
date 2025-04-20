package com.datastax.internal.utils.request;

import com.datastax.api.requests.objectaction.ObjectCreateAction;
import com.datastax.api.utils.request.ObjectCreateRequest;
import com.datastax.internal.utils.Checks;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ObjectCreateBuilder extends AbstractObjectBuilder<ObjectCreateBuilder> implements ObjectCreateRequest<ObjectCreateBuilder>
{
    @Nonnull
    @Override
    public ObjectCreateBuilder setContent(@Nullable String content)
    {
        if (content != null)
        {
            content = content.trim();
            this.content.setLength(0);
            this.content.append(content);
        }
        else
        {
            this.content.setLength(0);
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
    public <R> ObjectCreateBuilder addValues(@Nonnull Collection<? super R> values)
    {
        Checks.notNull(values, "Values");
        return this;
    }

    @Nonnull
    @Override
    public <R> ObjectCreateBuilder addValues(@Nonnull Map<String, ? super R> values)
    {
        Checks.notNull(values, "Values");
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setMaxBufferSize(int bufferSize)
    {
        Checks.notNegative(bufferSize, "The buffer size");
        this.maxBufferSize = bufferSize;
        this.objectFlags |= ObjectCreateAction.ObjectFlags.PAGE_SIZE.getValue();
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setNonce(long timestamp)
    {
        Checks.notNegative(timestamp, "Nonce");
        this.nonce = timestamp;
        this.objectFlags |= ObjectCreateAction.ObjectFlags.DEFAULT_TIMESTAMP.getValue();
        return this;
    }

    @Nonnull
    public ObjectCreateData build()
    {
        String content = this.content.toString().trim();
        int flags = this.objectFlags;
        int bufferSize = this.maxBufferSize;
        List<ByteBuf> values = this.values;

        return new ObjectCreateData(null, content, values, ObjectCreateAction.Consistency.ONE, flags, bufferSize);
    }
}
