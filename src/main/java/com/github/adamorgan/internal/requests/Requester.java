package com.github.adamorgan.internal.requests;

import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.Work;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.concurrent.CountingThreadFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Requester
{
    private final LibraryImpl library;
    private final StreamManager streamManager;

    public final Map<Short, Consumer<? super Response>> queue = new ConcurrentHashMap<>();
    public final Deque<WorkTask> requests = new ConcurrentLinkedDeque<>();

    public Requester(LibraryImpl library)
    {
        this.library = library;
        this.streamManager = new StreamManager((short) 50);
    }

    public <T> void execute(Request<T> request)
    {
        this.streamManager.execute(this.library, (api, stream) -> {
            this.execute(request, stream);
        });
    }

    @Nullable
    public ChannelHandlerContext getContext()
    {
        return this.library.getClient().getContext();
    }

    public <T> void execute(Request<T> request, short stream)
    {
        if (getContext() != null && !this.queue.containsKey(stream))
        {
            ByteBuf body = request.getBody();
            body.setShort(2, stream);
            request.handleResponse(stream, this.queue::put);

            getContext().writeAndFlush(body.retain());
        }
        else
        {
            requests.add(new WorkTask(this, request));
        }
    }

    public static class WorkTask implements Work
    {
        private final Requester requester;
        private final CaseInsensitiveMap<String, Integer> headers;
        private final ByteBuf body;
        private final Runnable callback;

        public <T> WorkTask(Requester requester, Request<T> request)
        {
            this.requester = requester;
            this.headers = request.getHeaders();
            this.body = request.getBody();
            this.callback = () -> this.requester.execute(request);
        }

        @Nonnull
        @Override
        public CaseInsensitiveMap<String, Integer> getHeaders()
        {
            return this.headers;
        }

        @Nonnull
        @Override
        public ByteBuf getBody()
        {
            return this.body;
        }

        public void execute()
        {
            this.callback.run();
        }
    }

    public static class StreamManager
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
    }
}
