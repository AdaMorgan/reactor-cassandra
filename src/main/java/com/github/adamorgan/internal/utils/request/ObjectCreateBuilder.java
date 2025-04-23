package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.data.DataValue;
import com.github.adamorgan.api.utils.data.Pair;
import com.github.adamorgan.api.utils.request.ObjectCreateRequest;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.Helpers;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ObjectCreateBuilder extends AbstractObjectBuilder<ObjectCreateBuilder> implements ObjectCreateRequest<ObjectCreateBuilder>
{
    @Nonnull
    @Override
    public <R> ObjectCreateBuilder setContent(@Nullable String content, @Nonnull Collection<? super R> args)
    {
        Checks.notNull(values, "Values");
        Helpers.setContent(this.content, content);
        this.fields |= ObjectCreateAction.Fields.VALUES.getValue();
        args.stream().map(DataValue::new).map(DataValue::asByteBuf).forEach(this.values::add);
        return this;
    }

    @Nonnull
    @Override
    public <R> ObjectCreateBuilder setContent(@Nullable String content, @Nonnull Map<String, ? super R> args)
    {
        Checks.notNull(values, "Values");
        Helpers.setContent(this.content, content);
        this.fields |= ObjectCreateAction.Fields.VALUES.getValue();
        this.fields |= ObjectCreateAction.Fields.VALUE_NAMES.getValue();
        args.entrySet().stream().map(Pair::new).map(Pair::asByteBuf).forEach(this.values::add);
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
    public ObjectCreateBuilder setMaxBufferSize(int bufferSize)
    {
        Checks.notNegative(bufferSize, "The buffer size");
        this.maxBufferSize = bufferSize;
        this.fields |= ObjectCreateAction.Fields.PAGE_SIZE.getValue();
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setNonce(long timestamp)
    {
        Checks.notNegative(timestamp, "Nonce");
        this.nonce = timestamp;
        this.fields |= ObjectCreateAction.Fields.DEFAULT_TIMESTAMP.getValue();
        return this;
    }

    @Nonnull
    public ObjectCreateData build()
    {
        String content = this.content.toString().trim();
        int flags = this.fields;
        int bufferSize = this.maxBufferSize;
        List<ByteBuf> values = this.values;

        return new ObjectCreateData(null, content, values, ObjectCreateAction.Consistency.ONE, flags, bufferSize);
    }
}
