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

public abstract class Event implements GenericEvent
{
    protected final Library api;
    protected final long responseNumber;

    /**
     * Creates a new Event from the given {@link Library} instance
     *
     * @param api
     *        Current {@link Library} instance
     * @param responseNumber
     *        The sequence number for this event
     *
     * @see   #Event(Library)
     */
    public Event(@Nonnull Library api, long responseNumber)
    {
        this.api = api;
        this.responseNumber = responseNumber;
    }

    /**
     * Creates a new Event from the given {@link Library} instance
     * <br>Uses the current {@link Library#getResponseTotal()} as sequence
     *
     * @param api
     *        Current {@link Library} instance
     */
    public Event(@Nonnull Library api)
    {
        this(api, api.getResponseTotal());
    }

    @Nonnull
    @Override
    public Library getLibrary()
    {
        return this.api;
    }

    @Override
    public long getResponseNumber()
    {
        return this.responseNumber;
    }
}
