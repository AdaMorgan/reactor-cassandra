package com.datastax.test;

import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.utils.concurrent.CountingThreadFactory;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

public class StreamManager
{
    protected final Queue<Short> queue = new ConcurrentLinkedQueue<>();
    protected final ExecutorService executor;

    public StreamManager(short totalStreams)
    {
        this.executor = Executors.newScheduledThreadPool(totalStreams, new CountingThreadFactory(() -> "Stream", "Worker"));

        for (short i = 0; i < totalStreams; i++)
        {
            queue.add(i);
        }
    }

    public <T> void execute(@Nonnull LibraryImpl api, @Nonnull BiConsumer<LibraryImpl, Short> command)
    {
        CompletableFuture<T> future = new CompletableFuture<>();

        Short streamId = queue.poll();
        if (streamId == null)
        {
            future.completeExceptionally(new IllegalStateException("No available streams"));
            return;
        }

        executor.execute(() ->
        {
            try
            {
                command.accept(api, streamId);
            }
            catch (Exception e)
            {
                future.completeExceptionally(e);
            }
            finally
            {
                queue.add(streamId);
            }
        });

    }

    public boolean contain(short streamId)
    {
        return this.queue.contains(streamId);
    }

    public void shutdown()
    {
        executor.shutdown();
    }
}