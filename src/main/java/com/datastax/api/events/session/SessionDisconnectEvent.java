package com.datastax.api.events.session;

import com.datastax.api.Library;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;

public class SessionDisconnectEvent extends GenericSessionEvent
{
    protected final OffsetDateTime disconnectTime;

    public SessionDisconnectEvent(@Nonnull Library api, OffsetDateTime disconnectTime)
    {
        super(api, SessionState.DISCONNECTED);
        this.disconnectTime = disconnectTime;
    }

    public OffsetDateTime getTimeDisconnect()
    {
        return disconnectTime;
    }
}
