package com.github.adamorgan.api.events.scheduled;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.events.Event;
import com.github.adamorgan.api.events.ScheduledEvent;

import javax.annotation.Nonnull;

public abstract class GenericScheduledEvent extends Event implements ScheduledEvent
{
    protected final Type type;

    public GenericScheduledEvent(Library api, Type type)
    {
        super(api);
        this.type = type;
    }

    @Nonnull
    @Override
    public Type getType()
    {
        return this.type;
    }
}
