package com.datastax.internal.utils;

public final class Helpers
{
    public static <T extends Throwable> T appendCause(T throwable, Throwable cause)
    {
        Throwable t = throwable;
        while (t.getCause() != null)
            t = t.getCause();
        t.initCause(cause);
        return throwable;
    }

    public static int codePointLength(final CharSequence string)
    {
        return (int) string.codePoints().count();
    }
}
