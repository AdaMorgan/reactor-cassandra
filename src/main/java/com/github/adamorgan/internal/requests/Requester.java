/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.adamorgan.internal.requests;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.exceptions.ErrorResponse;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.Work;
import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.LibraryLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Requester extends LinkedBlockingQueue<Integer> implements BlockingQueue<Integer>
{
    public static final Logger LOG = LibraryLogger.getLog(Requester.class);

    public static final int MIN = 1;
    public static final int MAX = 32768;

    private final CompletableFuture<?> shutdownHandle = new CompletableFuture<>();

    private boolean isStopped, isShutdown;

    protected final ReentrantLock lock = new ReentrantLock();
    protected final LibraryImpl api;

    protected final LinkedBlockingQueue<Integer> hitRateLimit = new LinkedBlockingQueue<>(MAX);
    protected final Map<Integer, WorkTask> queue = new ConcurrentHashMap<>(MAX);
    protected final LinkedBlockingQueue<WorkTask> rateLimitQueue = new LinkedBlockingQueue<>();

    public Requester(LibraryImpl api)
    {
        this.api = api;

        for (int i = MIN; i < MAX; i++)
        {
            this.hitRateLimit.add(i);
        }
    }

    @Nullable
    public ChannelHandlerContext getContext()
    {
        return this.api.getClient().getContext();
    }

    public <R> void request(@Nonnull Request<R> request)
    {
        WorkTask task = new WorkTask(request);

        if (getContext() != null && request.getBody().getShort(2) != 0)
        {
            execute(getContext(), task);
        }
        else
        {
            this.rateLimitQueue.add(task);
        }
    }

    private void execute(@Nonnull ChannelHandlerContext context, @Nonnull WorkTask task)
    {
        ObjectAction<?> objectAction = task.request.getObjectAction();

        queue.put((int) task.getBody().getShort(2), task);

        try
        {
            LOG.trace("Executing request {}", ByteBufUtil.prettyHexDump(task.request.getBody()));

            context.writeAndFlush(task.getBody().retain()).addListener(result ->
            {
                if (!result.isSuccess())
                {
                    result.cause().printStackTrace();
                }
            }).await(objectAction.getDeadline(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException timeout)
        {
            task.request.onTimeout();
        }
    }

    public void enqueue(ChannelHandlerContext context, byte version, byte flags, int stream, byte opcode, int length, ErrorResponse failure, ByteBuf body)
    {
        UUID trace = ObjectAction.Flags.fromBitField(flags).contains(ObjectAction.Flags.TRACING) ? new UUID(body.readLong(), body.readLong()) : null;

        WorkTask task = queue.remove(stream);

        if (task != null) {
            task.handleResponse(new Response(version, flags, stream, opcode, length, failure, body, trace));
        } else {
            LOG.warn("Received response for unknown stream id {}", stream);
        }

        body.release();

        if (!this.rateLimitQueue.isEmpty())
        {
            execute(context, this.rateLimitQueue.poll());
        }
        else
        {
            this.offer(stream);
        }
    }

    @Override
    public boolean offer(@Nonnull Integer id)
    {
        Checks.notNull(id, "id");
        Checks.inRange(id, 0, MAX, "id");
        return MiscUtil.locked(lock, () -> this.hitRateLimit.offer(id));
    }

    @Nonnull
    @Override
    public Integer poll()
    {
        return MiscUtil.locked(lock, () -> {
            Integer poll = this.hitRateLimit.poll();
            return poll != null ? poll : 0;
        });
    }

    @Override
    public int size()
    {
        return this.queue.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.queue.isEmpty();
    }

    @Override
    public int remainingCapacity()
    {
        return MAX - size();
    }

    @Override
    public void clear()
    {
        MiscUtil.locked(lock, this.queue::clear);
    }

    public void stop(boolean shutdown, Runnable callback)
    {
        MiscUtil.locked(lock, () -> {
            boolean doShutdown = shutdown;
            if (!isStopped)
            {
                isStopped = true;
                shutdownHandle.thenRun(callback);
                if (!doShutdown)
                {
                    int count = this.rateLimitQueue.size() + this.queue.size();

                    if (count > 0)
                    {
                        LOG.info("Waiting for {} requests to finish.", count);
                    }
                    doShutdown = count == 0;
                }
            }

            if (doShutdown && !isShutdown)
                shutdown();
        });
    }

    private void shutdown()
    {
        isShutdown = true;
        this.clear();
        shutdownHandle.complete(null);
    }

    public class WorkTask implements Work
    {
        private final Request<?> request;

        private boolean done;

        public WorkTask(@Nonnull Request<?> request)
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
            Requester.this.request(request);
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
            return isSkipped() || done;
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