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
import com.github.adamorgan.api.requests.*;
import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.UnlockHook;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Requester implements RequestManager
{
    public static final Logger LOG = LibraryLogger.getLog(Requester.class);

    private final LibraryImpl api;

    private final CompletableFuture<Void> shutdownHandle = new CompletableFuture<>();

    private boolean isStopped, isShutdown;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<Integer, Requester.WorkTask> hitRateLimit = new ConcurrentHashMap<>();

    private final IntObjectMap<Bucket> buckets = new IntObjectHashMap<>();
    private final Map<Bucket, Future<?>> rateLimitQueue = new HashMap<>();

    private final SocketClient client;

    private final Future<?> cleanupWorker;

    public Requester(@Nonnull LibraryImpl api)
    {
        this.api = api;
        this.client = api.getClient();
        this.cleanupWorker = api.getCallbackPool().schedule(this::cleanup, 30, TimeUnit.SECONDS);
    }

    @Nonnull
    public LibraryImpl getLibrary()
    {
        return api;
    }

    public <R> void request(@Nonnull Request<R> request)
    {
        if (isStopped || isShutdown)
            request.onFailure(new RejectedExecutionException("The Requester has been stopped! No new requests can be requested!"));

        if (request.shouldQueue())
            enqueue(new WorkTask(request));
    }

    @Override
    public void enqueue(@Nonnull WorkTask task)
    {
        try (UnlockHook hook = writeLock())
        {
            Bucket bucket = getBucket(task);
            bucket.enqueue(task);
            runBucket(bucket);
        }
    }

    public void handleResponse(@Nonnull ChannelHandlerContext context, byte flags, int stream, byte opcode, int length, Exception exception, ByteBuf body)
    {
        try
        {
            long rawData = ((long) flags << 56) | ((long) stream << 40) | ((long) opcode << 32) | length;

            Bucket bucket = buckets.get(stream);

            WorkTask mainTask = hitRateLimit.remove(stream);

            //the error is within the driver implementation
            if (mainTask == null)
            {
                throw new IOException("Target resource is no longer available at the origin server");
            }

            ByteBuf retainedBody = body.retain();

            mainTask.handleResponse(context, rawData, exception, retainedBody.duplicate());

            Iterator<WorkTask> it = bucket.requests.iterator();
            while (it.hasNext())
            {
                WorkTask req = it.next();
                if (mainTask.equals(req))
                {
                    it.remove();
                    req.handleResponse(context, rawData, exception, retainedBody.duplicate());
                }
            }

            retainedBody.release();

            bucket.backoff();
        }
        catch (IOException failure)
        {
            failure.printStackTrace();
        }
        finally
        {
            body.release();
        }
    }

    public void stop(boolean shutdown, @Nonnull Runnable callback)
    {
        try (UnlockHook hook = readLock())
        {
            boolean doShutdown = shutdown;
            if (!isStopped)
            {
                isStopped = true;
                shutdownHandle.thenRun(callback);
                if (!doShutdown)
                {
                    int count = buckets.values().stream()
                            .mapToInt(bucket -> bucket.getRequests().size())
                            .sum();

                    if (count > 0)
                    {
                        LOG.info("Waiting for {} requests to finish.", count);
                    }

                    doShutdown = count == 0;
                }
            }
            if (doShutdown && !isShutdown)
                shutdown();
        }
    }

    @Override
    public boolean isStopped()
    {
        return isStopped;
    }

    public UnlockHook writeLock()
    {
        if (lock.getReadHoldCount() > 0)
            throw new IllegalStateException("Unable to acquire write-lock while holding read-lock!");
        Lock writeLock = lock.writeLock();
        MiscUtil.tryLock(writeLock);
        return new UnlockHook(writeLock);
    }

    public UnlockHook readLock()
    {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        MiscUtil.tryLock(readLock);
        return new UnlockHook(readLock);
    }

    private void shutdown()
    {
        isShutdown = true;
        cleanupWorker.cancel(false);
        cleanup();
        shutdownHandle.complete(null);
    }

    private Bucket getBucket(@Nonnull Work request)
    {
        try (UnlockHook hook = readLock())
        {
            int bucketId = request.hashCode();
            return this.buckets.computeIfAbsent(bucketId, Bucket::new);
        }
    }

    private void runBucket(Bucket bucket)
    {
        try (UnlockHook hook = readLock())
        {
            if (isShutdown)
                return;

            // Schedule a new bucket worker if no worker is running
            bucket.run();
        }
    }

    private void cleanup()
    {
        // This will remove buckets that are no longer needed every 30 seconds to avoid memory leakage
        // We will keep the hashes in memory since they are very limited (by the amount of possible routes)
        try (UnlockHook hook = readLock())
        {
            int size = buckets.size();
            Iterator<Map.Entry<Integer, Bucket>> entries = buckets.entrySet().iterator();

            while (entries.hasNext())
            {
                Map.Entry<Integer, Bucket> entry = entries.next();
                Bucket bucket = entry.getValue();
                if (isShutdown)
                    bucket.requests.forEach(Work::cancel); // Cancel all requests
                bucket.requests.removeIf(Work::isSkipped); // Remove cancelled requests

                // Check if the bucket is empty
                if (bucket.requests.isEmpty() && !rateLimitQueue.containsKey(bucket))
                {
                    // Remove empty buckets when the rate limiter is stopped
                    if (isStopped)
                        entries.remove();
                }
            }

            // LOG how many buckets were removed
            size -= buckets.size();
            if (size > 0)
                LOG.debug("Removed {} expired buckets", size);
            else if (isStopped && !isShutdown)
                shutdown();
        }
    }

    @Override
    public int cancelRequests()
    {
        try (UnlockHook hook = readLock())
        {
            int cancelled = (int) buckets.values()
                    .stream()
                    .map(Bucket::getRequests)
                    .flatMap(Collection::stream)
                    .filter(request -> !request.isPriority() && !request.isCancelled())
                    .peek(Work::cancel)
                    .count();

            if (cancelled == 1)
                LOG.warn("Cancelled 1 request!");
            else if (cancelled > 1)
                LOG.warn("Cancelled {} requests!", cancelled);
            return cancelled;
        }
    }

    private class Bucket implements Runnable
    {
        protected final int bucketId;
        protected final Deque<WorkTask> requests = new ConcurrentLinkedDeque<>();

        public Bucket(int bucketId)
        {
            this.bucketId = bucketId;
        }

        public void enqueue(@Nonnull WorkTask request)
        {
            requests.addLast(request);
        }

        public void retry(@Nonnull WorkTask request)
        {
            if (!moveRequest(request))
                requests.addFirst(request);
        }

        protected void backoff()
        {
            // Schedule backoff if requests are not done
            try (UnlockHook hook = readLock())
            {
                if (!requests.isEmpty())
                    runBucket(this);
                else if (isStopped)
                    buckets.remove(bucketId);
                else
                    rateLimitQueue.remove(this);
                if (isStopped && buckets.isEmpty())
                    shutdown();
            }
        }

        @Nonnull
        public Queue<WorkTask> getRequests()
        {
            return requests;
        }

        protected boolean moveRequest(@Nonnull WorkTask request)
        {
            try (UnlockHook hook = writeLock())
            {
                Bucket bucket = getBucket(request);
                if (bucket != this)
                {
                    bucket.enqueue(request);
                    runBucket(bucket);
                }
                return bucket != this;
            }
        }

        @Override
        public void run()
        {
            try (UnlockHook hook = readLock())
            {
                if (hitRateLimit.containsKey(bucketId) || requests.isEmpty())
                    return;

                WorkTask request = requests.removeFirst();

                if (request.isCancelled())
                    return;

                hitRateLimit.put(bucketId, request);
                ByteBuf body = request.request.getBody().asByteBuf();
                client.context.writeAndFlush(body.retain());
            }
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this)
                return true;
            if (!(obj instanceof Bucket))
                return false;
            return this.bucketId == ((Bucket) obj).bucketId;
        }
    }

    public class WorkTask implements RequestManager.Work
    {
        protected final Request<?> request;
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
        public boolean isPriority()
        {
            return false;
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

        public void handleResponse(@Nonnull ChannelHandlerContext context, long rawData, Exception exception, ByteBuf body)
        {
            done = true;
            request.handleResponse(new Response(context, rawData, exception, body));
        }

        public void handleResponse(@Nonnull ChannelHandlerContext context, Exception exception)
        {
            done = true;
            request.handleResponse(new Response(context, 0, exception, Unpooled.EMPTY_BUFFER));
        }

        @Override
        public int hashCode()
        {
            return request.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof WorkTask))
                return false;
            if (obj == this)
                return true;

            WorkTask other = (WorkTask) obj;
            return hashCode() == other.hashCode();
        }
    }
}