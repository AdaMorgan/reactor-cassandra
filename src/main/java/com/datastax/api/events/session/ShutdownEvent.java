package com.datastax.api.events.session;

import com.datastax.api.Library;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;

public class ShutdownEvent extends GenericSessionEvent
{
    protected final OffsetDateTime shutdownTime;

    public ShutdownEvent(@Nonnull Library api, @Nonnull OffsetDateTime shutdownTime)
    {
        super(api, SessionState.SHUTDOWN);
        this.shutdownTime = shutdownTime;
    }

    public OffsetDateTime getTimeShutdown()
    {
        return shutdownTime;
    }
}
