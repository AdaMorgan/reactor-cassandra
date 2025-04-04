package com.datastax.internal;

import com.datastax.api.Library;
import com.datastax.api.events.GenericEvent;
import com.datastax.api.events.StatusChangeEvent;
import com.datastax.api.hooks.IEventManager;
import com.datastax.api.hooks.ListenerAdapter;
import com.datastax.api.utils.MiscUtil;
import com.datastax.internal.hooks.EventManagerProxy;
import com.datastax.internal.requests.Requester;
import com.datastax.internal.utils.Checks;
import com.datastax.internal.utils.CustomLogger;
import com.datastax.internal.utils.config.ThreadingConfig;
import com.datastax.test.SocketClient;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LibraryImpl implements Library
{
    public static final Logger LOG = CustomLogger.getLog(Library.class);

    protected final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZING);
    protected final ReentrantLock statusLock = new ReentrantLock();
    protected final Condition statusCondition = statusLock.newCondition();

    protected final AtomicInteger responseTotal = new AtomicInteger(0);

    protected final byte[] token;

    protected final Requester requester;
    protected final ThreadingConfig threadingConfig;
    protected final EventManagerProxy eventManager;
    protected final SocketClient client;

    public LibraryImpl(byte[] token, ThreadingConfig threadingConfig, IEventManager eventManager)
    {
        this.token = token;
        this.client = new SocketClient(this);
        this.requester = new Requester(this);
        this.threadingConfig = threadingConfig;
        this.eventManager = new EventManagerProxy(eventManager, threadingConfig.getEventPool());
    }

    public SocketClient getClient()
    {
        return client;
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

    @Override
    public void addEventListener(@Nonnull ListenerAdapter... listeners)
    {
        Checks.noneNull(listeners, "listeners");

        for (ListenerAdapter listener : listeners)
        {
            eventManager.register(listener);
        }
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
    public List<? extends ListenerAdapter> getRegisteredListeners()
    {
        return eventManager.getRegisteredListeners();
    }

    @Override
    public long getResponseTotal()
    {
        return responseTotal.get();
    }

    @Nonnull
    @Override
    public ExecutorService getCallbackPool()
    {
        return threadingConfig.getCallbackPool();
    }

    public void handleEvent(@Nonnull GenericEvent event)
    {
        eventManager.handle(event);
    }

    public Requester getRequester()
    {
        return this.requester;
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
}
