package com.github.adamorgan.internal.requests;

import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.Work;
import com.github.adamorgan.internal.LibraryImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Requester
{
    private final LibraryImpl library;

    public final Map<Short, Consumer<? super Response>> queue = new ConcurrentHashMap<>();
    public final Deque<WorkTask> requests = new ConcurrentLinkedDeque<>();

    public Requester(LibraryImpl library)
    {
        this.library = library;
    }

    @Nullable
    public ChannelHandlerContext getContext()
    {
        return this.library.getClient().getContext();
    }

    public <R> void execute(@Nonnull Request<R> request)
    {
        short streamId = (short) request.getShardId();

        if (getContext() != null && !this.queue.containsKey(streamId))
        {
            request.handleResponse(streamId, this.queue::put);
            getContext().writeAndFlush(request.getBody().retain());
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

        public <R> WorkTask(Requester requester, Request<R> request)
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
}
