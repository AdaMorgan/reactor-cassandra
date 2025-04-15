package com.datastax.api.events.session;

import com.datastax.api.Library;

import javax.annotation.Nonnull;

public class ReadyEvent extends GenericSessionEvent
{
    public ReadyEvent(@Nonnull Library api)
    {
        super(api, SessionState.READY);
    }
}
