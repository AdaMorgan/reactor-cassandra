package com.datastax.internal.requests;

import com.datastax.api.audit.ThreadLocalReason;
import com.datastax.api.requests.ObjectAction;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class ObjectActionImpl<T> implements ObjectAction<T>
{
    public static final Logger LOG = LoggerFactory.getLogger(ObjectActionImpl.class);

    private final String localReason;

    protected final LibraryImpl api;
    protected final byte version, flags, opcode;

    protected final BiFunction<Request<T>, Response, T> handler;

    public ObjectActionImpl(LibraryImpl api, byte version, byte flags, byte opcode, BiFunction<Request<T>, Response, T> handler)
    {
        this.api = api;
        this.version = version;
        this.flags = flags;
        this.opcode = opcode;
        this.handler = handler;

        this.localReason = ThreadLocalReason.getCurrent();
    }

    public ObjectActionImpl(LibraryImpl api, byte version, byte flags, byte opcode)
    {
        this(api, version, flags, opcode, null);
    }

    public void queue(Consumer<? super T> success)
    {
        this.queue(success, null);
    }

    public void queue(Consumer<? super T> success, Consumer<? super Throwable> failure)
    {
        ByteBuf body = applyData();
        api.getRequester().execute(new Request<>(this, body, success, failure));
    }

    protected void handleSuccess(Request<T> request, Response response)
    {
        T successObj = handler == null ? null : handler.apply(request, response);
        request.onSuccess(successObj);
    }

    public void handleResponse(Request<T> request, Response response)
    {
        try (ThreadLocalReason.Closable __ = ThreadLocalReason.closable(localReason))
        {
            handleSuccess(request, response);
        }
    }
}
