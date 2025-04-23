package com.github.adamorgan.api.events.session;

import com.github.adamorgan.api.Library;

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

    @Nonnull
    public OffsetDateTime getTimeShutdown()
    {
        return shutdownTime;
    }
}
