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

package com.github.adamorgan.api.hooks;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.events.Event;
import com.github.adamorgan.api.events.GenericEvent;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.Checks;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An {@link IEventManager IEventManager} implementation
 * that uses the {@link EventListener EventListener} interface for
 * event listeners.
 *
 * <p>This only accepts listeners that implement {@link EventListener EventListener}
 * <br>An adapter implementation is {@link ListenerAdapter ListenerAdapter} which
 * provides methods for each individual {@link Event Event}.
 *
 * <p><b>This is the default IEventManager used by {@link Library}</b>
 *
 * @see IEventManager
 */
public class InterfacedEventManager implements IEventManager
{
    private final CopyOnWriteArrayList<ListenerAdapter> listeners = new CopyOnWriteArrayList<>();

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *         If the provided listener does not implement {@link java.util.EventListener EventListener}
     */
    @Override
    public void register(@Nonnull ListenerAdapter listener)
    {
        Checks.notNull(listener, "listener");
        listeners.add(listener);
    }

    @Override
    public void unregister(@Nonnull ListenerAdapter listener)
    {
        Checks.notNull(listener, "listener");
        listeners.remove(listener);
    }

    @Nonnull
    @Override
    @Unmodifiable
    public List<? extends ListenerAdapter> getRegisteredListeners()
    {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public void handle(@Nonnull GenericEvent event)
    {
        for (EventListener listener : listeners)
        {
            try
            {
                listener.onEvent(event);
            }
            catch (Throwable throwable)
            {
                LibraryImpl.LOG.error("One of the EventListeners had an uncaught exception", throwable);
                if (throwable instanceof Error)
                    throw (Error) throwable;
            }
        }
    }
}
