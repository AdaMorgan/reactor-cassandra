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
import com.github.adamorgan.api.LibraryBuilder;
import com.github.adamorgan.api.events.GenericEvent;
import com.github.adamorgan.internal.LibraryImpl;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * An interface for {@link LibraryImpl} EventManager system.
 * <br>This should be registered in the {@link LibraryBuilder LibraryBuilder}
 *
 * <p>{@link Library} provides 2 implementations:
 * <ul>
 *     <li>{@link InterfacedEventManager InterfacedEventManager}
 *     <br>Simple implementation that allows {@link EventListener EventListener}
 *         instances as listeners.</li>
 * </ul>
 *
 * <p>The default event manager is {@link InterfacedEventManager InterfacedEventManager}
 * <br>Use {@link LibraryBuilder#setEventManager(IEventManager) LibraryBuilder#setEventManager(IEventManager)}
 * to set the preferred event manager implementation.
 * <br>You can only use one implementation per {@link Library} instance!
 *
 * @see InterfacedEventManager
 */
public interface IEventManager
{
    /**
     * Registers the specified listener
     * <br>Accepted types may be specified by implementations
     *
     * @param listener
     *        A listener object
     *
     * @throws UnsupportedOperationException
     *         If the implementation does not support this method
     */
    void register(@Nonnull ListenerAdapter listener);

    /**
     * Removes the specified listener
     *
     * @param listener
     *        The listener object to remove
     *
     * @throws UnsupportedOperationException
     *         If the implementation does not support this method
     */
    void unregister(@Nonnull ListenerAdapter listener);

    /**
     * Handles the provided {@link GenericEvent GenericEvent}.
     * <br>How this is handled is specified by the implementation.
     *
     * <p>An implementation should not throw exceptions.
     *
     * @param event
     *        The event to handle
     */
    void handle(@Nonnull GenericEvent event);

    /**
     * The currently registered listeners
     *
     * @throws UnsupportedOperationException
     *         If the implementation does not support this method
     *
     * @return A list of listeners that have already been registered
     */
    @Nonnull
    List<? extends ListenerAdapter> getRegisteredListeners();
}
