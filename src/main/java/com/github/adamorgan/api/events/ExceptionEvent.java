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

public class ExceptionEvent extends Event
{
    protected final Throwable throwable;
    protected final boolean logged;

    public ExceptionEvent(@Nonnull Library api, @Nonnull Throwable throwable, boolean logged)
    {
        super(api);
        this.throwable = throwable;
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
        return throwable;
    }
}
