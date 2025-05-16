package com.github.adamorgan.api.requests;

import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.action.ObjectActionImpl;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

public class ObjectFuture<T> extends CompletableFuture<T>
{
    protected final Request<T> request;
    protected final ObjectActionImpl<T> action;

    public ObjectFuture(ObjectActionImpl<T> action, ByteBuf body, long deadline)
    {
        this.action = action;
        this.request = new Request<>(action, body, this::complete, this::completeExceptionally, deadline);

        ((LibraryImpl) this.action.getLibrary()).getRequester().execute(this.request);
    }

    @Override
    public boolean cancel(final boolean mayInterrupt)
    {
        return (!isDone() && !isCancelled()) && super.cancel(mayInterrupt);
    }
}
