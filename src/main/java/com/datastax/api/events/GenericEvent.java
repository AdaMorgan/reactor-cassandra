package com.datastax.api.events;

import com.datastax.api.Library;

import javax.annotation.Nonnull;

public interface GenericEvent
{
    /**
     * The current {@link Library} instance corresponding to this Event
     *
     * @return The corresponding {@link Library} instance
     */
    @Nonnull
    Library getLibrary();
    
    /**
     * The current sequence for this event.
     * <br>This can be used to keep events in order when making sequencing system.
     *
     * @return The current sequence number for this event
     */
    long getResponseNumber();
}
