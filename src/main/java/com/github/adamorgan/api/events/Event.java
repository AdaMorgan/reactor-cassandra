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
