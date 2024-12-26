package com.datastax.internal.objectaction;

import com.datastax.api.request.ObjectFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class ObjectActionImpl<T> implements ObjectAction<T>
{
    public static final Logger LOG = LoggerFactory.getLogger(ObjectAction.class);

    private final ObjectFactory connectionFactory;
    private final String route;
    private final BiFunction<Request<T>, Response, T> handler;

    public ObjectActionImpl(ObjectFactory connectionFactory, String route, BiFunction<Request<T>, Response, T> handler)
    {
        this.connectionFactory = connectionFactory;
        this.route = route;
        this.handler = handler;
    }

    public ObjectFactory getObjectFactory()
    {
        return connectionFactory;
    }

    @Override
    public String getRoute()
    {
        return route;
    }

    @Override
    public void queue()
    {

    }

    @Override
    public void complete()
    {

    }

    @Override
    public CompletableFuture<T> submit()
    {
        return new ObjectFuture<>(this);
    }

    protected void handleSuccess(Request<T> request, Response response)
    {
        request.onSuccess(handler == null ? null : handler.apply(request, response));
    }
}