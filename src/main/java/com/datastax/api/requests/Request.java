package com.datastax.api.requests;

import com.datastax.api.exceptions.ErrorResponse;
import com.datastax.api.exceptions.ErrorResponseException;
import com.datastax.internal.requests.action.ObjectActionImpl;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Request<T>
{
    protected final ObjectActionImpl<T> objAction;
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
