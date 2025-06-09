package com.github.adamorgan.internal.requests;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.Work;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.LibraryLogger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Requester
{
    public static final Logger LOG = LibraryLogger.getLog(Requester.class);

    private final LibraryImpl library;

    private final Map<Short, Request<?>> queue = new ConcurrentHashMap<>();

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
        if (getContext() != null)
        {
            short streamId = (short) request.getObjectAction().getStreamId();

            queue.put(streamId, request);
            getContext().writeAndFlush(request.getBody().retain());
        }
        else
        {
            throw new IllegalStateException("Premature execution of a request");
        }
    }

    public void enqueue(byte version, byte flags, short stream, byte opcode, int length, ByteBuf body)
    {
        Request<?> request = queue.remove(stream);

        if (request != null)
        {
            this.library.release(stream);
            request.handleResponse(new Response(version, flags, stream, opcode, length, body));
            body.release();
        }
        else
        {
            body.release();
        }
    }

    public class WorkTask implements Work
    {
        private final Request<?> request;

        private boolean done;

        public WorkTask(Request<?> request)
        {
            this.request = request;
        }

        @Nonnull
        @Override
        public Library getLibrary()
        {
            return this.request.getLibrary();
        }

        @Override
        public void execute()
        {
            Requester.this.execute(request);
        }

        @Nonnull
        @Override
        public ByteBuf getBody()
        {
            return request.getBody();
        }

        @Override
        public boolean isSkipped()
        {
            return request.isSkipped();
        }

        @Override
        public boolean isDone()
        {
            return request.isSkipped() || done;
        }

        @Override
        public boolean isCancelled()
        {
            return request.isCancelled();
        }

        @Override
        public void cancel()
        {
            request.cancel();
        }

        public void handleResponse(Response response)
        {
            done = true;
            request.handleResponse(response);
        }
    }
}