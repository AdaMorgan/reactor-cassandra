package com.datastax.internal.requests;

import com.datastax.api.requests.ObjectAction;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class ObjectActionImpl<T> implements ObjectAction<T>
{
    protected final LibraryImpl api;
    protected final int version, flags, opcode;

    protected final short stream;

    private final BiFunction<Request<T>, Response, T> handler;

    public ObjectActionImpl(LibraryImpl api, int version, int flags, short stream, int opcode, BiFunction<Request<T>, Response, T> handler)
    {
        this.api = api;
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = opcode;
        this.handler = handler;
    }

    public void queue(Consumer<? super T> success)
    {
        this.queue(success, null);
    }

    public void queue(Consumer<? super T> success, Consumer<? super Throwable> failure)
    {
        ByteBuf body = finalizeBuffer();
        api.getRequester().execute(new Request<>(this, body, success, failure));
    }

    public ByteBuf finalizeBuffer()
    {
        return Unpooled.directBuffer();
    }

    public ByteBuf applyData()
    {
        return null;
    }

    protected void handleSuccess(Request<T> request, Response response)
    {
        T successObj = handler == null ? null : handler.apply(request, response);
        request.onSuccess(successObj);
    }

    public void handleResponse(Request<T> request, Response response)
    {
        handleSuccess(request, response);
    }

    public int getFlags()
    {
        return this.flags;
    }

    public short getStreamId()
    {
        return this.stream;
    }

    public final int getCode()
    {
        return this.opcode;
    }
}
