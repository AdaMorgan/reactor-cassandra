package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.exceptions.ErrorResponseException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class ObjectActionImpl<T> implements ObjectAction<T>
{
    public static final Logger LOG = LibraryLogger.getLog(ObjectActionImpl.class);

    private static final Consumer<Object> DEFAULT_SUCCESS = o -> {};
    private static final Consumer<? super Throwable> DEFAULT_FAILURE = t -> LOG.error("ObjectAction queue returned failure: [{}] {}", t.getClass().getSimpleName(), t.getMessage());

    protected final LibraryImpl api;

    protected final int stream;

    protected final byte version;

    protected final BiFunction<Request<T>, Response, T> handler;

    //TODO-Failure: TimeoutException should be thrown in queue(onFailure)
    public ObjectActionImpl(LibraryImpl api, BiFunction<Request<T>, Response, T> handler)
    {
        this.api = api;
        this.version = api.getVersion();
        this.handler = handler;

        try
        {
            this.stream = api.acquire(1000, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e)
        {
            throw new IllegalStateException("Request timed out");
        }
    }

    public ObjectActionImpl(LibraryImpl api)
    {
        this(api, null);
    }

    @Nonnull
    @Override
    public Library getLibrary()
    {
        return this.api;
    }

    @Override
    public int getStreamId()
    {
        return this.stream;
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
        ByteBuf body = this.finalizeData().applyData();

        if (success == null)
        {
            success = DEFAULT_SUCCESS;
        }
        if (failure == null)
        {
            failure = DEFAULT_FAILURE;
        }

        api.getRequester().request(new Request<>(this, body, success, failure, getDeadline()));
    }

    @Override
    public T complete(boolean shouldQueue)
    {
        try
        {
            return submit(shouldQueue).join();
        }
        catch (CompletionException failException)
        {
            if (failException.getCause() != null)
            {
                Throwable cause = failException.getCause();
                if (cause instanceof ErrorResponseException)
                    throw (ErrorResponseException) cause.fillInStackTrace();
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                if (cause instanceof Error)
                    throw (Error) cause;
            }
            throw failException;
        }
    }

    @Nonnull
    @Override
    public CompletableFuture<T> submit(boolean shouldQueue)
    {
        ByteBuf body = this.finalizeData().applyData();
        return new ObjectFuture<>(this, body, getDeadline());
    }

    public void handleResponse(Request<T> request, Response response)
    {
        if (response.isOk())
        {
            handleSuccess(request, response);
        }
        else
        {
            request.onFailure(response.getException());
        }
    }

    //TODO: replace void with CallbackRunnable
    protected void handleSuccess(Request<T> request, Response response)
    {
        T successObj = handler == null ? null : handler.apply(request, response);
        request.onSuccess(successObj);
    }

    public long getDeadline()
    {
        return 0;
    }
}
