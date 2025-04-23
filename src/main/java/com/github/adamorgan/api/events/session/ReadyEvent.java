package com.github.adamorgan.api.events.session;

import com.github.adamorgan.api.Library;

import javax.annotation.Nonnull;

public class ReadyEvent extends GenericSessionEvent
{
    public ReadyEvent(@Nonnull Library api)
    {
        super(api, SessionState.READY);
    }
}
