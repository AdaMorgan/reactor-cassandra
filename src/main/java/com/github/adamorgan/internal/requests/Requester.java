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
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.Work;
import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.Helpers;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.UnlockHook;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.ObjectUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Requester extends LinkedBlockingQueue<Integer> implements BlockingQueue<Integer>
{
    public static final Logger LOG = LibraryLogger.getLog(Requester.class);

    public static final int MIN = 1;
    public static final int MAX = 32768;

    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final LibraryImpl library;

    protected final LinkedBlockingQueue<Integer> hitRateLimit = new LinkedBlockingQueue<>(MAX - 1);
    protected final TIntObjectMap<WorkTask> queue = new TIntObjectHashMap<>(MAX);
    protected final LinkedBlockingQueue<WorkTask> rateLimitQueue = new LinkedBlockingQueue<>();

    public Requester(LibraryImpl library)
    {
        this.library = library;

        for (int i = 1; i < MAX; i++)
        {
            this.hitRateLimit.add(i);
        }
    }

    @Nullable
    public ChannelHandlerContext getContext()
    {
        return this.library.getClient().getContext();
    }

    public <R> void request(@Nonnull Request<R> request)
    {
        request(new WorkTask(request));
    }

    public void request(@Nonnull WorkTask request)
    {
        if (getContext() != null && remainingCapacity() > 0)
        {
            execute(getContext(), request);
        }
        else
        {
            this.rateLimitQueue.add(request);
        }
    }

    private void execute(@Nonnull ChannelHandlerContext context, @Nonnull WorkTask task)
    {
        ObjectAction<?> objectAction = task.request.getObjectAction();
        final int id = task.getBody().getShort(2);

        queue.put(id, task);

        try
        {
            LOG.trace("Executing request {}", ByteBufUtil.prettyHexDump(task.request.getBody()));

            context.writeAndFlush(task.getBody().retain()).addListener(result ->
            {
                if (!result.isSuccess())
                {
                    queue.remove(id);
                }
            }).await(objectAction.getDeadline(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException timeout)
        {
            task.request.onTimeout();
        }
    }

    public void enqueue(byte version, byte flags, int stream, byte opcode, int length, ErrorResponse failure, ByteBuf body)
    {
        UUID trace = ObjectAction.Flags.fromBitField(flags).contains(ObjectAction.Flags.TRACING) ? new UUID(body.readLong(), body.readLong()) : null;
        queue.remove(stream).handleResponse(new Response(version, flags, stream, opcode, length, failure, body, trace));

        body.release();

        this.offer(stream);

        if (!this.rateLimitQueue.isEmpty())
        {
            this.rateLimitQueue.poll();
        }
    }

    @Override
    public boolean offer(@Nonnull Integer id)
    {
        Checks.notNull(id, "id");
        Checks.inRange(id, 0, MAX, "id");
        try (UnlockHook hook = writeLock())
        {
            return this.hitRateLimit.offer(id);
        }
    }

    @Nonnull
    @Override
    public Integer poll()
    {
        try (UnlockHook hook = writeLock())
        {
            Integer poll = this.hitRateLimit.poll();
            return poll != null ? poll : 0;
        }
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
        try (UnlockHook hook = writeLock())
        {
            this.queue.clear();
        }
    }

    public void stop(boolean shutdown, Runnable callback)
    {
        this.clear();
    }

    private UnlockHook writeLock()
    {
        if (lock.getReadHoldCount() > 0)
        {
            throw new IllegalStateException("Unable to acquire write-lock while holding read-lock!");
        }
        Lock writeLock = lock.writeLock();
        MiscUtil.tryLock(writeLock);
        return new UnlockHook(writeLock);
    }

    private UnlockHook readLock()
    {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        MiscUtil.tryLock(readLock);
        return new UnlockHook(readLock);
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