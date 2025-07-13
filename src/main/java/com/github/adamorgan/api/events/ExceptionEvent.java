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

package com.github.adamorgan.api.events;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.internal.utils.LibraryLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * Indicates that Library encountered a {@link Throwable Throwable} that could not be forwarded to another end-user frontend.
 * <br>For instance this is fired for events in internal {@link com.github.adamorgan.internal.requests.SocketClient Socket} handling.
 *
 * <p>It is not recommended to simply use this and print each event as some fails were already logged. See {@link #isLogged()}.
 */
public class ExceptionEvent extends Event
{
    protected final Throwable failure;
    protected final boolean logged;

    public ExceptionEvent(@Nonnull Library api, @Nonnull Throwable failure, boolean logged)
    {
        super(api);
        this.failure = failure;
        this.logged = logged;
    }

    /**
     * Whether this Throwable was already printed using the {@link LibraryLogger}
     *
     * @return True, if this throwable was already logged
     */
    public boolean isLogged()
    {
        return logged;
    }

    /**
     * The cause Throwable for this event
     *
     * @return The cause
     */
    @Nonnull
    public Throwable getCause()
    {
        return failure;
    }

    /**
     * Returns the detail message string of this Throwable.
     *
     * @return The detail message string of this {@code Throwable} instance
     */
    @Nullable
    public String getMessage()
    {
        return failure.getMessage();
    }
}
