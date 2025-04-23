package com.github.adamorgan.api.requests;

import com.github.adamorgan.api.exceptions.ErrorResponse;
import com.github.adamorgan.api.exceptions.ErrorResponseException;
import com.github.adamorgan.internal.requests.action.ObjectActionImpl;
import io.netty.buffer.ByteBuf;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Request<T>
{
    protected final ObjectActionImpl<T> objAction;
    protected final CaseInsensitiveMap<String, Integer> headers = new CaseInsensitiveMap<>();
    protected final ByteBuf body;
    protected final Consumer<? super T> onSuccess;
    protected final Consumer<? super Throwable> onFailure;

    public Request(ObjectActionImpl<T> objAction, ByteBuf body, Consumer<? super T> onSuccess, Consumer<? super Throwable> onFailure)
    {
        this.objAction = objAction;
        this.body = body;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Nonnull
    public CaseInsensitiveMap<String, Integer> getHeaders()
    {
        return headers;
    }

    @Nonnull
    public ByteBuf getBody()
    {
        return body;
    }

    public void onSuccess(T successObj)
    {
        this.onSuccess.accept(successObj);
    }

    public void onFailure(Throwable failException)
    {
        this.onFailure.accept(failException);
    }

    private void handleResponse(Response response)
    {
        if (response.isError())
        {
            this.onFailure(createErrorResponseException(response));
        }
        else
        {
            this.objAction.handleResponse(this, response);
        }
    }

    public void handleResponse(short stream, BiConsumer<? super Short, Consumer<? super Response>> handler)
    {
        handler.accept(stream, this::handleResponse);
    }

    @Nonnull
    public ErrorResponseException createErrorResponseException(@Nonnull Response response)
    {
        return ErrorResponseException.create(ErrorResponse.fromBuffer(response.getBody()), response);
    }
}
