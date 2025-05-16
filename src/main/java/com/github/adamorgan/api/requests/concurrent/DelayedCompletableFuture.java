package com.github.adamorgan.api.requests.concurrent;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Specialized {@link CompletableFuture} used in combination with a scheduler.
 *
 * @param <T> The result type
 * @see CompletableFuture
 * @see Delayed
 */
public class DelayedCompletableFuture<T> extends CompletableFuture<T> implements ScheduledFuture<T>
{
    private ScheduledFuture<?> future;

    private DelayedCompletableFuture()
    {
    }

    /**
     * Creates a new DelayedCompletableFuture scheduled on the supplied executor.
     *
     * @param executor The {@link ScheduledExecutorService} to use for scheduling
     * @param delay    The delay of the task
     * @param unit     Conversion {@link TimeUnit} for the delay
     * @param mapping  Conversion function which calls {@link #complete(Object)} of the future it receives
     * @param <E>      The result type of the scheduled task
     * @return DelayedCompletableFuture for the specified runnable
     */
    @Nonnull
    @CheckReturnValue
    public static <E> DelayedCompletableFuture<E> make(@Nonnull ScheduledExecutorService executor, long delay, @Nonnull TimeUnit unit, @Nonnull Function<? super DelayedCompletableFuture<E>, ? extends Runnable> mapping)
    {
        DelayedCompletableFuture<E> handle = new DelayedCompletableFuture<>();
        ScheduledFuture<?> future = executor.schedule(mapping.apply(handle), delay, unit);
        handle.initProxy(future);
        return handle;
    }

    /**
     * Initializes the backing scheduled task for this promise.
     *
     * <p>The provided future will be cancelled when {@link #cancel(boolean)} is invoked
     * and is used as provider for {@link #getDelay(TimeUnit)}.
     *
     * @param future The future that should be cancelled when this task is cancelled
     * @throws IllegalStateException If this was already initialized
     */
    private void initProxy(ScheduledFuture<?> future)
    {
        if (this.future == null)
        {
            this.future = future;
        }
        else
        {
            throw new IllegalStateException("Cannot initialize twice");
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        if (future != null && !future.isDone())
        {
            future.cancel(mayInterruptIfRunning);
        }
        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public long getDelay(@Nonnull TimeUnit unit)
    {
        return future.getDelay(unit);
    }

    @Override
    public int compareTo(@Nonnull Delayed o)
    {
        return future.compareTo(o);
    }
}
