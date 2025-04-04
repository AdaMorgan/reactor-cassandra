package com.datastax.api.requests;

import com.datastax.internal.requests.ObjectActionImpl;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nullable;
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

    public final int getFlags()
    {
        return this.objAction.getFlags();
    }

    public final short getStreamId()
    {
        return this.objAction.getStreamId();
    }

    public final int getCode()
    {
        return this.objAction.getCode();
    }

    public final ByteBuf applyData()
    {
        return this.objAction.applyData();
    }

    @Nullable
    public ByteBuf getBody()
    {
        return body;
    }

    public void onSuccess(T successObj)
    {
        this.onSuccess.accept(successObj);
    }

    public void onFailure(Response response)
    {

    }

    public void onFailure(Throwable failException)
    {
        this.onFailure.accept(failException);
    }

    private void handleResponse(Response response)
    {
        this.objAction.handleResponse(this, response);
    }

    public void handleResponse(BiConsumer<? super Short, Consumer<? super Response>> handler)
    {
        handler.accept(this.getStreamId(), this::handleResponse);
    }
}
