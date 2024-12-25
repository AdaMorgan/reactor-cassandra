package com.datastax.api.audit;

import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;

public final class ThreadLocalReason
{
    private static ThreadLocal<String> currentReason;

    private ThreadLocalReason()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the current reason that should be used for {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction}.
     *
     * @param reason
     *        The reason to use, or {@code null} to reset
     */
    public static void setCurrent(@Nullable String reason)
    {
        if (reason != null)
        {
            if (currentReason == null)
                currentReason = new ThreadLocal<>();
            currentReason.set(reason);
        }
        else if (currentReason != null)
        {
            currentReason.remove();
        }
    }

    /**
     * Resets the currently set thread-local reason, if present.
     */
    public static void resetCurrent()
    {
        if (currentReason != null)
            currentReason.remove();
    }

    /**
     * The current reason that should be used for {@link net.dv8tion.jda.api.requests.restaction.AuditableRestAction AuditableRestAction}.
     *
     * @return The current thread-local reason, or null
     */
    @Nullable
    public static String getCurrent()
    {
        return currentReason == null ? null : currentReason.get();
    }

    /**
     * Creates a new {@link Closable} instance.
     * <br>Allows to use try-with-resources blocks for setting reasons
     *
     * @param  reason
     *         The reason to use
     *
     * @return The closable instance
     */
    @Nonnull
    public static Closable closable(@Nullable String reason)
    {
        return new Closable(reason);
    }

    /**
     * Allows to use try-with-resources blocks for setting reasons
     */
    public static class Closable implements AutoCloseable
    {
        private final String previous;

        public Closable(@Nullable String reason)
        {
            this.previous = getCurrent();
            setCurrent(reason);
        }

        @Override
        public void close()
        {
            setCurrent(previous);
        }
    }
}
