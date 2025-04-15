package com.datastax.api.events.session;

import com.datastax.api.Library;
import com.datastax.api.events.Event;

import javax.annotation.Nonnull;

public abstract class GenericSessionEvent extends Event
{
    protected final SessionState state;

    public GenericSessionEvent(@Nonnull Library api, @Nonnull SessionState state)
    {
        super(api);
        this.state = state;
    }

    @Nonnull
    public SessionState getState()
    {
        return state;
    }
}
