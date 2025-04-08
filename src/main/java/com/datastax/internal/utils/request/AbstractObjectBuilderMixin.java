package com.datastax.internal.utils.request;

import com.datastax.api.utils.request.ObjectCreateRequest;

public interface AbstractObjectBuilderMixin<T extends ObjectCreateRequest<T>> extends ObjectCreateRequest<T>
{
}
