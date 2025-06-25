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

package com.github.adamorgan.internal;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.LibraryInfo;
import com.github.adamorgan.api.events.GenericEvent;
import com.github.adamorgan.api.events.StatusChangeEvent;
import com.github.adamorgan.api.hooks.IEventManager;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.hooks.EventManagerProxy;
import com.github.adamorgan.internal.requests.Requester;
import com.github.adamorgan.internal.requests.SocketClient;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.cache.ObjectCacheViewImpl;
import com.github.adamorgan.internal.utils.config.SessionConfig;
import com.github.adamorgan.internal.utils.config.ThreadingConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LibraryImpl implements Library
{
    public static final Logger LOG = LibraryLogger.getLog(Library.class);

    protected final ObjectCacheViewImpl objCache = new ObjectCacheViewImpl(ByteBuf.class);

    protected final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZING);
    protected final ReentrantLock statusLock = new ReentrantLock();
    protected final Condition statusCondition = statusLock.newCondition();

    protected final AtomicInteger responseTotal = new AtomicInteger(0);

    protected final byte[] token;
    protected final ShardInfo shardInfo;
    protected final SessionConfig sessionConfig;

    protected final Thread shutdownHook;

    protected final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(32768, false);

    protected final Requester requester;
    protected final ThreadingConfig threadConfig;
    protected final EventManagerProxy eventManager;
    protected final SocketClient client;

    public LibraryImpl(final byte[] token, final SocketAddress address, final Compression compression, final ShardInfo shardInfo, final ThreadingConfig threadConfig, final SessionConfig sessionConfig, final IEventManager eventManager)
    {
        this.token = token;
        this.threadConfig = threadConfig;
        this.sessionConfig = sessionConfig;
        this.shardInfo = shardInfo;
        this.shutdownHook = sessionConfig.isUseShutdownHook() ? new Thread(this::shutdownNow, "Library Shutdown Hook") : null;
        this.client = new SocketClient(this, address, compression, sessionConfig);
        this.requester = new Requester(this);
        this.eventManager = new EventManagerProxy(eventManager, threadConfig.getEventPool());

        for (int i = 1; i <= 32768; i++)
        {
            this.queue.add(i);
        }
    }

    public SocketClient getClient()
    {
        return client;
    }

    @Nonnull
    @Override
    public Compression getCompression()
    {
        return this.client.getCompression();
    }

    @Nonnull
    @Override
    public byte[] getToken()
    {
        return token;
    }

    @Nonnull
    @Override
    public Status getStatus()
    {
        return status.get();
    }

    public boolean isEventPassthrough()
    {
        return sessionConfig.isEventPassthrough();
    }

    @Override
    public boolean isDebug()
    {
        return sessionConfig.isDebug();
    }

    @Override
    public boolean isAutoReconnect()
    {
        return sessionConfig.isAutoReconnect();
    }

    @Nonnull
    @Override
    public ShardInfo getShardInfo()
    {
        return shardInfo == null ? ShardInfo.SINGLE : shardInfo;
    }

    @Override
    public void addEventListener(@Nonnull ListenerAdapter... listeners)
    {
        Checks.noneNull(listeners, "listeners");

        for (ListenerAdapter listener : listeners)
        {
            eventManager.register(listener);
        }
    }

    @Nonnull
    @Override
    public ObjectCacheViewImpl getObjectCache()
    {
        return this.objCache;
    }

    @Override
    public void removeEventListener(@Nonnull ListenerAdapter... listeners)
    {
        Checks.noneNull(listeners, "listeners");

        for (ListenerAdapter listener : listeners)
        {
            eventManager.unregister(listener);
        }
    }

    @Nonnull
    @Override
    @Unmodifiable
    public List<? extends ListenerAdapter> getRegisteredListeners()
    {
        return eventManager.getRegisteredListeners();
    }

    @Nonnull
    public SessionController getSessionController()
    {
        return sessionConfig.getSessionController();
    }

    @Override
    public int getMaxReconnectDelay()
    {
        return sessionConfig.getMaxReconnectDelay();
    }

    @Override
    public long getResponseTotal()
    {
        return responseTotal.get();
    }

    @Nonnull
    @Override
    public EventLoopGroup getCallbackPool()
    {
        return threadConfig.getCallbackPool();
    }

    public void handleEvent(@Nonnull GenericEvent event)
    {
        eventManager.handle(event);
    }

    public Requester getRequester()
    {
        return this.requester;
    }

    public int getMaxBufferSize()
    {
        return sessionConfig.getMaxBufferSize();
    }

    //TODO-Failure: Make an implementation in which ObjectAction will wait for its turn
    public int acquire()
    {
        return this.queue.poll();
    }

    public void release(int id)
    {
        this.queue.offer(id);
    }

    public void setStatus(Status status)
    {
        StatusChangeEvent event = MiscUtil.locked(statusLock, () ->
        {
            Status oldStatus = this.status.getAndSet(status);
            this.statusCondition.signalAll();

            return new StatusChangeEvent(this, status, oldStatus);
        });

        if (event.getOldStatus() != event.getNewStatus())
        {
            handleEvent(event);
        }
    }

    public byte getVersion()
    {
        return LibraryInfo.PROTOCOL_VERSION;
    }

    public synchronized void connect()
    {
        if (shutdownHook != null)
            Runtime.getRuntime().addShutdownHook(shutdownHook);

        this.client.connect();
    }

    @Override
    public synchronized void shutdownNow()
    {
        shutdown();
        threadConfig.shutdownNow();
    }

    @Override
    public synchronized void shutdown()
    {
        Status status = getStatus();
        if (status == Status.SHUTDOWN || status == Status.SHUTTING_DOWN)
            return;

        if (shutdownHook != null)
            Runtime.getRuntime().removeShutdownHook(shutdownHook);

        setStatus(Status.SHUTTING_DOWN);

        this.client.shutdown();
    }
}
