package com.datastax.api.request;

import com.datastax.internal.ObjectFactoryImpl;
import com.datastax.internal.objectaction.ObjectActionImpl;
import com.datastax.internal.requests.Request;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link CompletableFuture} used for {@link ObjectAction#submit()}.
 *
 * @param <T> The result type
 */
public class ObjectFuture<T> extends CompletableFuture<T>
{
    protected final Request<T> request;

    public ObjectFuture(final ObjectActionImpl<T> restAction, final boolean shouldQueue)
    {
        this.request = new Request<>(restAction, shouldQueue, this::complete, this::completeExceptionally);

        ((ObjectFactoryImpl) restAction.getObjectFactory()).getRequester().execute(request);
    }

    @Override
    public boolean cancel(final boolean mayInterrupt)
    {
        return (!isDone() && !isCancelled()) && super.cancel(mayInterrupt);
    }
}
