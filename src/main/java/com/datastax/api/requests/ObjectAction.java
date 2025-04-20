package com.datastax.api.requests;

import com.datastax.api.Library;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.internal.requests.action.operator.MapObjectAction;
import com.datastax.internal.utils.Checks;
import io.netty.buffer.ByteBuf;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ObjectAction<T>
{
    @Nonnull
    Library getLibrary();

    @Nonnull
    ByteBuf asByteBuf();

    default void queue()
    {
        this.queue(null);
    }

    default void queue(@Nullable Consumer<? super T> success)
    {
        this.queue(success, null);
    }

    void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<T> submit(boolean shouldQueue);

    @Nonnull
    @CheckReturnValue
    default CompletableFuture<T> submit()
    {
        return submit(false);
    }

    @Nonnull
    static Consumer<? super Throwable> getDefaultFailure()
    {
        return ObjectActionImpl.getDefaultFailure();
    }

    @Nonnull
    static Consumer<Object> getDefaultSuccess()
    {
        return ObjectActionImpl.getDefaultSuccess();
    }

    @Nonnull
    @CheckReturnValue
    default <O> ObjectAction<O> map(@Nonnull Function<? super T, ? extends O> map)
    {
        Checks.notNull(map, "Function");
        return new MapObjectAction<>(this, map);
    }
}
