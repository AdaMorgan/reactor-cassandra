package com.datastax.internal.utils.request;

import com.datastax.api.utils.request.ObjectCreateRequest;
import com.datastax.internal.utils.Checks;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

public class ObjectCreateBuilder extends AbstractObjectBuilder<ObjectCreateBuilder> implements ObjectCreateRequest<ObjectCreateBuilder>
{
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
}
