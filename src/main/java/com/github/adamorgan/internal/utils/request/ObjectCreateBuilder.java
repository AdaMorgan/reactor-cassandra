package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.data.DataValue;
import com.github.adamorgan.api.utils.data.Pair;
import com.github.adamorgan.api.utils.request.ObjectCreateRequest;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.Helpers;
import com.github.adamorgan.internal.utils.collections.TTextByteHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public class ObjectCreateBuilder extends AbstractObjectBuilder<ObjectCreateBuilder> implements ObjectCreateRequest<ObjectCreateBuilder>
{
    @Nonnull
    @Override
    public <R> ObjectCreateBuilder setContent(@Nullable String content, @Nonnull Collection<? super R> args)
    {
        Checks.notNull(values, "Values");
        Helpers.setContent(this.content, content);
        this.values.clear();
        if (!args.isEmpty())
        {
            this.fields |= ObjectCreateAction.Field.VALUES.getRawValue();
            args.stream().map(DataValue::new).map(DataValue::asByteBuf).forEach(this.values::add);
        }
        return this;
    }

    @Nonnull
    @Override
    public <R> ObjectCreateBuilder setContent(@Nullable String content, @Nonnull Map<String, ? super R> args)
    {
        Checks.notNull(values, "Values");
        Helpers.setContent(this.content, content);
        this.values.clear();
        this.fields |= ObjectCreateAction.Field.VALUES.getRawValue();
        this.fields |= ObjectCreateAction.Field.VALUE_NAMES.getRawValue();
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
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setNonce(long timestamp)
    {
        Checks.notNegative(timestamp, "Nonce");
        this.nonce = timestamp;
        this.fields |= ObjectCreateAction.Field.DEFAULT_TIMESTAMP.getRawValue();
        return this;
    }
}
