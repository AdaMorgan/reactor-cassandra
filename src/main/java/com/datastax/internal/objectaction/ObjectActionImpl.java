package com.datastax.internal.objectaction;

import com.datastax.api.ObjectFactory;
import com.datastax.api.request.ObjectAction;
import com.datastax.api.request.ObjectFuture;
import com.datastax.internal.ObjectFactoryImpl;
import com.datastax.internal.request.ObjectRoute;
import com.datastax.internal.request.Request;
import com.datastax.internal.request.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ObjectActionImpl<T> implements ObjectAction<T>
{
    public static final Logger LOG = LoggerFactory.getLogger(ObjectActionImpl.class);

    public static final Consumer<? super Object> DEFAULT_SUCCESS = o -> {};

    private static final Consumer<? super Throwable> DEFAULT_FAILURE = error -> {
        if (error instanceof TimeoutException) {
            LOG.debug(error.getMessage());
        } else {
            LOG.error("RestAction queue returned failure: [{}] {}", error.getClass().getSimpleName(), error.getMessage());
        }
    };

    private final ObjectFactory connectionFactory;
    private final ObjectRoute.CompileRoute route;
    private final BiFunction<Request<T>, Response, T> handler;

    public ObjectActionImpl(ObjectFactory connectionFactory, ObjectRoute.CompileRoute route, BiFunction<Request<T>, Response, T> handler)
    {
        this.connectionFactory = connectionFactory;
        this.route = route;
        this.handler = handler;
    }

    public ObjectFactory getObjectFactory()
    {
        return connectionFactory;
    }

    public String finalizeRoute()
    {
        return route.getCompiledRoute();
    }

    @Override
    public void queue(Consumer<? super T> success, Consumer<? super Throwable> failure)
    {
        if (success == null)
            success = DEFAULT_SUCCESS;
        if (failure == null)
            failure = DEFAULT_FAILURE;

        Request<T> request = new Request<>(this, success, failure);

        ((ObjectFactoryImpl) this.connectionFactory).getRequester().execute(request);
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
    public CompletableFuture<T> submit()
    {
        return new ObjectFuture<>(this);
    }

    public static boolean isPassContext()
    {
        return true;
    }

    public void handleSuccess(Request<T> request, Response response)
    {
        request.onSuccess(handler == null ? null : handler.apply(request, response));
    }
}