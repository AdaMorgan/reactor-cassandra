package com.github.adamorgan.api.hooks;

import com.github.adamorgan.api.events.*;
import com.github.adamorgan.api.events.binary.BinaryRequestEvent;
import com.github.adamorgan.api.events.scheduled.GenericScheduledEvent;
import com.github.adamorgan.api.events.session.GenericSessionEvent;
import com.github.adamorgan.api.events.session.ReadyEvent;
import com.github.adamorgan.api.events.session.SessionDisconnectEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.internal.utils.ClassWalker;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ListenerAdapter implements EventListener
{
    public void onGenericEvent(@Nonnull GenericEvent event) {}
    public void onGenericUpdate(@Nonnull UpdateEvent<?, ?> event) {}

    public void onStatusChange(@Nonnull StatusChangeEvent event) {}

    public void onReady(@Nonnull ReadyEvent event) {}
    public void onSessionDisconnect(@Nonnull SessionDisconnectEvent event) {}
    public void onShutdown(@Nonnull ShutdownEvent event) {}

    public void onBinaryRequest(@Nonnull BinaryRequestEvent event) {}

    public void onException(@Nonnull ExceptionEvent event) {}

    //Generic Events
    public void onGenericSession(@Nonnull GenericSessionEvent event) {}
    public void onGenericChangeEvent(@Nonnull GenericScheduledEvent event) {}

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final ConcurrentMap<Class<?>, MethodHandle> methods = new ConcurrentHashMap<>();
    private static final Set<Class<?>> unresolved;

    static
    {
        unresolved = ConcurrentHashMap.newKeySet();
        Collections.addAll(unresolved,
                Object.class, // Objects aren't events
                Event.class, // onEvent is final and would never be found
                UpdateEvent.class, // onGenericUpdate has already been called
                GenericEvent.class // onGenericEvent has already been called
        );
    }

    @Override
    public final void onEvent(@Nonnull GenericEvent event)
    {
        onGenericEvent(event);
        if (event instanceof UpdateEvent)
            onGenericUpdate((UpdateEvent<?, ?>) event);

        for (Class<?> clazz : ClassWalker.range(event.getClass(), GenericEvent.class))
        {
            if (unresolved.contains(clazz))
                continue;
            MethodHandle mh = methods.computeIfAbsent(clazz, ListenerAdapter::findMethod);
            if (mh == null)
            {
                unresolved.add(clazz);
                continue;
            }

            try
            {
                mh.invoke(this, event);
            }
            catch (Throwable throwable)
            {
                if (throwable instanceof RuntimeException)
                    throw (RuntimeException) throwable;
                if (throwable instanceof Error)
                    throw (Error) throwable;
                throw new IllegalStateException(throwable);
            }
        }
    }

    @Nullable
    private static MethodHandle findMethod(@Nonnull Class<?> clazz)
    {
        String name = clazz.getSimpleName();
        MethodType type = MethodType.methodType(Void.TYPE, clazz);
        try
        {
            name = "on" + name.substring(0, name.length() - "Event".length());

            return lookup.findVirtual(ListenerAdapter.class, name, type);
        }
        catch (NoSuchMethodException | IllegalAccessException ignored) // this means this is probably a custom event!
        {
            return null;
        }
    }
}
