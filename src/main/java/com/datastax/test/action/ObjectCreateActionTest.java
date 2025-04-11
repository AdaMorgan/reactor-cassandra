package com.datastax.test.action;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.requests.action.CacheObjectAction;
import com.datastax.api.requests.objectaction.ObjectCreateAction;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.internal.utils.request.ObjectCreateBuilder;
import com.datastax.internal.utils.request.ObjectCreateBuilderMixin;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class ObjectCreateActionTest extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>, CacheObjectAction<ByteBuf>
{
    public ObjectCreateBuilder builder = new ObjectCreateBuilder();

    public ObjectCreateActionTest(LibraryImpl api, byte version, byte flags)
    {
        super(api, version, flags, SocketCode.QUERY);
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        return null;
    }

    @Nonnull
    @Override
    public ObjectCreateAction addContent(@Nonnull String content)
    {
        return null;
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder getBuilder()
    {
        return this.builder;
    }

    @Override
    protected void handleSuccess(@Nonnull Request<ByteBuf> request, @Nonnull Response response)
    {
        request.onSuccess(response.getBody());
    }
}
