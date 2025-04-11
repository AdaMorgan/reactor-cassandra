package com.datastax.api.requests.objectaction;

import com.datastax.api.requests.ObjectAction;
import com.datastax.api.utils.request.ObjectCreateRequest;
import com.datastax.internal.utils.request.ObjectCreateBuilderMixin;
import io.netty.buffer.ByteBuf;

public interface ObjectCreateAction extends ObjectAction<ByteBuf>, ObjectCreateRequest<ObjectCreateAction>
{
}
