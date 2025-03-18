package com.datastax.api.events;

import com.datastax.api.Library;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Indicates that our {@link Library.Status Status} changed. (Example: SHUTTING_DOWN {@literal ->} SHUTDOWN)
 *
 * <br>Can be used to detect internal status changes. Possibly to log or forward on user's end.
 *
 * <p>Identifier: {@code status}
 */
public class StatusChangeEvent extends Event implements UpdateEvent<Library, Library.Status>
{
    public static final String IDENTIFIER = "status";

    private final Library.Status newStatus;
    private final Library.Status oldStatus;

    public StatusChangeEvent(@Nonnull Library api, @Nonnull Library.Status newStatus, @Nonnull Library.Status oldStatus)
    {
        super(api);
        this.newStatus = newStatus;
        this.oldStatus = oldStatus;
    }

    /**
     * The status that we changed to
     *
     * @return The new status
     */
    @Nonnull
    public Library.Status getNewStatus()
    {
        return newStatus;
    }

    /**
     * The previous status
     *
     * @return The previous status
     */
    @Nonnull
    public Library.Status getOldStatus()
    {
        return oldStatus;
    }

    @Nonnull
    @Override
    public String getPropertyIdentifier()
    {
        return IDENTIFIER;
    }

    @Nonnull
    @Override
    public Library getEntity()
    {
        return getLibrary();
    }

    @Nullable
    @Override
    public Library.Status getOldValue()
    {
        return oldStatus;
    }

    @Nullable
    @Override
    public Library.Status getNewValue()
    {
        return newStatus;
    }
}
