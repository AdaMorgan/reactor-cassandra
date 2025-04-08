package com.datastax.internal.utils.request;

import com.datastax.api.utils.request.ObjectCreateData;
import com.datastax.api.utils.request.ObjectCreateRequest;
import com.datastax.internal.utils.Checks;

import javax.annotation.Nonnull;

public class ObjectCreateBuilder extends AbstractObjectBuilder<ObjectCreateData, ObjectCreateBuilder> implements ObjectCreateRequest<ObjectCreateBuilder>
{
    @Nonnull
    @Override
    public ObjectCreateBuilder addContent(@Nonnull String content)
    {
        Checks.notNull(content, "Content");
        this.content.append(content);
        return this;
    }
}
