package com.datastax.api.events;

import com.datastax.api.ObjectFactory;
import org.example.data.DataObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface GenericEvent
{
    @Nonnull
    ObjectFactory getObjectFactory();

    /**
     * The current sequence for this event.
     * <br>This can be used to keep events in order when making sequencing system.
     *
     * @return The current sequence number for this event
     */
    long getResponseNumber();

    @Nullable
    DataObject getRawData();
}
