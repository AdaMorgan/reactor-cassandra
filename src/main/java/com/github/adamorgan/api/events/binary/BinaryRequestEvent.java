package com.github.adamorgan.api.events.binary;

import com.github.adamorgan.api.events.Event;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import io.netty.buffer.ByteBuf;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BinaryRequestEvent extends Event
{
    private final Request<?> request;
    private final Response response;

    public BinaryRequestEvent(@Nonnull final Request<?> request, @Nonnull final Response response)
    {
        super(request.getLibrary());
        this.request = request;
        this.response = response;
    }

    @Nonnull
    public Request<?> getRequest()
    {
        return request;
    }

    @Nullable
    public ByteBuf getRequestBody()
    {
        return !this.api.isDebug() ? this.request.getBody() : null;
    }

    @Nonnull
    public Response getResponse()
    {
        return response;
    }

    @Nonnull
    public ByteBuf getResponseBody()
    {
        return this.response.getBody();
    }

    @Nonnull
    @CheckReturnValue
    public ObjectAction<?> getObjectAction()
    {
        return this.request.getObjectAction();
    }
}
