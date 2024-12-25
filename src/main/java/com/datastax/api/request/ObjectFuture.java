package com.datastax.api.request;

import com.datastax.internal.objectaction.ObjectActionImpl;
import com.datastax.internal.objectaction.Request;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link CompletableFuture} used for {@link com.datastax.internal.objectaction.ObjectAction#submit()}.
 *
 * @param <T> The result type
 */
public class ObjectFuture<T> extends CompletableFuture<T>
{
    final Request<T> request;

    public ObjectFuture(final ObjectActionImpl<T> restAction)
    {
        this.request = new Request<>(restAction, this::complete, this::completeExceptionally);
    }

    @Override
    public boolean cancel(final boolean mayInterrupt)
    {
        return (!isDone() && !isCancelled()) && super.cancel(mayInterrupt);
    }
}
