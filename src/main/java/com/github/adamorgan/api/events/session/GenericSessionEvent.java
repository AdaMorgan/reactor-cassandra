package com.github.adamorgan.api.events.session;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.events.Event;

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
