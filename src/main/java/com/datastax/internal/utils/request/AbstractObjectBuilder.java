package com.datastax.internal.utils.request;

import com.datastax.api.utils.request.ObjectRequest;

public abstract class AbstractObjectBuilder<T, R extends AbstractObjectBuilder<T, R>> implements ObjectRequest<R>
{
    protected final StringBuilder content = new StringBuilder();
}
