package com.github.adamorgan.internal.utils;

import java.math.BigDecimal;

public final class Helpers
{
    public static void setContent(StringBuilder builder, String content)
    {
        if (content != null)
        {
            content = content.trim();
            builder.setLength(0);
            builder.append(content);
        }
        else
        {
            builder.setLength(0);
        }
    }

    public static <T extends Throwable> T appendCause(T throwable, Throwable cause)
    {
        Throwable t = throwable;
        while (t.getCause() != null)
            t = t.getCause();
        t.initCause(cause);
        return throwable;
    }

    public static boolean isDecimal(String number)
    {
        try
        {
            new BigDecimal(number);
            return true;
        }
        catch (NumberFormatException failure)
        {
            return false;
        }
    }

    public static int codePointLength(final CharSequence string)
    {
        return (int) string.codePoints().count();
    }
}
