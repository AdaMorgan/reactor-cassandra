package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.utils.request.ObjectCreateRequest;

public interface ObjectCreateBuilderMixin<T extends ObjectCreateRequest<T>> extends AbstractObjectBuilderMixin<T, ObjectCreateBuilder>, ObjectCreateRequest<T>
{

}
