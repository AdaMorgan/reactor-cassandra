package com.github.adamorgan.api.events;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.internal.utils.LibraryLogger;

import javax.annotation.Nonnull;

public class ExceptionEvent extends Event
{
    protected final Throwable throwable;
    protected final boolean logged;

    public ExceptionEvent(@Nonnull Library api, @Nonnull Throwable throwable, boolean logged)
    {
        super(api);
        this.throwable = throwable;
        this.logged = logged;
    }

    /**
     * Whether this Throwable was already printed using the {@link LibraryLogger}
     *
     * @return True, if this throwable was already logged
     */
    public boolean isLogged()
    {
        return logged;
    }

    /**
     * The cause Throwable for this event
     *
     * @return The cause
     */
    @Nonnull
    public Throwable getCause()
    {
        return throwable;
    }
}
