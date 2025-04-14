package com.datastax.api.events;

import com.datastax.api.Library;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;

public class ShutdownEvent extends Event
{
    protected final OffsetDateTime shutdownTime;

    public ShutdownEvent(@Nonnull Library api, @Nonnull OffsetDateTime shutdownTime)
    {
        super(api);
        this.shutdownTime = shutdownTime;
    }

    public OffsetDateTime getTimeShutdown()
    {
        return shutdownTime;
    }
}
