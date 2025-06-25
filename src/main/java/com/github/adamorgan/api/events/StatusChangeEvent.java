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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Indicates that our {@link Library.Status Status} changed. (Example: SHUTTING_DOWN {@literal ->} SHUTDOWN)
 *
 * <br>Can be used to detect internal status changes. Possibly to log or forward on user's end.
 *
 * <p>Identifier: {@code status}
 */
public class StatusChangeEvent extends Event implements UpdateEvent<Library, Library.Status>
{
    public static final String IDENTIFIER = "status";

    private final Library.Status newStatus;
    private final Library.Status oldStatus;

    public StatusChangeEvent(@Nonnull Library api, @Nonnull Library.Status newStatus, @Nonnull Library.Status oldStatus)
    {
        super(api);
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
    }

    /**
     * The status that we changed to
     *
     * @return The new status
     */
    @Nonnull
    public Library.Status getNewStatus()
    {
        return newStatus;
    }

    /**
     * The previous status
     *
     * @return The previous status
     */
    @Nonnull
    public Library.Status getOldStatus()
    {
        return oldStatus;
    }

    @Nonnull
    @Override
    public String getPropertyIdentifier()
    {
        return IDENTIFIER;
    }

    @Nonnull
    @Override
    public Library getEntity()
    {
        return getLibrary();
    }

    @Nullable
    @Override
    public Library.Status getOldValue()
    {
        return oldStatus;
    }

    @Nullable
    @Override
    public Library.Status getNewValue()
    {
        return newStatus;
    }
}
