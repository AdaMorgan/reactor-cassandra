package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.audit.ThreadLocalReason;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.ObjectFuture;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.LibraryLogger;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class ObjectActionImpl<T> implements ObjectAction<T>
{
    public static final Logger LOG = LibraryLogger.getLog(ObjectActionImpl.class);

    private static final Consumer<Object> DEFAULT_SUCCESS = o -> {};
    private static final Consumer<? super Throwable> DEFAULT_FAILURE = t -> LOG.error("ObjectAction queue returned failure: [{}] {}", t.getClass().getSimpleName(), t.getMessage());

    protected final LibraryImpl api;

    protected final byte version, opcode;
    protected final int stream;
    protected int flags;

    private final String localReason;

    protected final BiFunction<Request<T>, Response, T> handler;

    public ObjectActionImpl(LibraryImpl api, byte opcode, BiFunction<Request<T>, Response, T> handler)
    {
        this.api = api;
        this.version = api.getVersion();
        this.stream = api.getShardInfo().getShardId();
        this.opcode = opcode;
        this.handler = handler;
        this.flags = 0x00;

        this.localReason = ThreadLocalReason.getCurrent();
    }

    public ObjectActionImpl(LibraryImpl api, byte opcode)
    {
        this(api, opcode, null);
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
    public int getFlagsRaw()
    {
        return this.flags;
    }

    @Nonnull
    @Override
    public EnumSet<Flags> getFlags()
    {
        return ObjectAction.Flags.fromBitField(this.flags);
    }

    @Override
    public void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure)
    {
        ByteBuf body = this.finalizeData();

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
        ByteBuf body = this.finalizeData();
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
