package com.datastax.api.audit;

public final class ThreadLocalReason
{
    private static ThreadLocal<String> currentReason;

    private ThreadLocalReason()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the current reason that should be used for {@link AuditableObjectAction AuditableObjectAction}.
     *
     * @param reason
     *        The reason to use, or {@code null} to reset
     */
    public static void setCurrent(String reason)
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
     * The current reason that should be used for {@link AuditableObjectAction AuditableObjectAction}.
     *
     * @return The current thread-local reason, or null
     */
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
    public static Closable closable(String reason)
    {
        return new Closable(reason);
    }

    /**
     * Allows to use try-with-resources blocks for setting reasons
     */
    public static class Closable implements AutoCloseable
    {
        private final String previous;

        public Closable(String reason)
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
