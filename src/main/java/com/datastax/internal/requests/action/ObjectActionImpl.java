package com.datastax.internal.requests.action;

import com.datastax.api.Library;
import com.datastax.api.audit.ThreadLocalReason;
import com.datastax.api.requests.ObjectAction;
import com.datastax.api.requests.ObjectFuture;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class ObjectActionImpl<T> implements ObjectAction<T>
{
    public static final Logger LOG = LoggerFactory.getLogger(ObjectActionImpl.class);

    private static final Consumer<Object> DEFAULT_SUCCESS = o -> {};
    private static final Consumer<? super Throwable> DEFAULT_FAILURE = t -> LOG.error("ObjectAction queue returned failure: [{}] {}", t.getClass().getSimpleName(), t.getMessage());

    protected final LibraryImpl api;

    protected final byte version, flags, opcode;

    private final String localReason;

    protected final BiFunction<Request<T>, Response, T> handler;

    public ObjectActionImpl(LibraryImpl api, byte flags, byte opcode, BiFunction<Request<T>, Response, T> handler)
    {
        this.api = api;
        this.version = api.getVersion();
        this.flags = flags;
        this.opcode = opcode;
        this.handler = handler;

        this.localReason = ThreadLocalReason.getCurrent();
    }

    public ObjectActionImpl(LibraryImpl api, byte flags, byte opcode)
    {
        this(api, flags, opcode, null);
    }

    @Nonnull
    @Override
    public Library getLibrary()
    {
        return this.api;
    }

    public static Consumer<? super Throwable> getDefaultFailure()
    {
        return DEFAULT_FAILURE;
    }

    public static Consumer<Object> getDefaultSuccess()
    {
        return DEFAULT_SUCCESS;
    }

    @Override
    public void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure)
    {
        ByteBuf body = this.asByteBuf();

        if (success == null)
        {
            success = DEFAULT_SUCCESS;
        }
        if (failure == null)
        {
            failure = DEFAULT_FAILURE;
        }

        api.getRequester().execute(new Request<>(this, body, success, failure));
    }

    @Nonnull
    @Override
    public CompletableFuture<T> submit(boolean shouldQueue)
    {
        ByteBuf body = this.asByteBuf();
        return new ObjectFuture<>(this, body);
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
