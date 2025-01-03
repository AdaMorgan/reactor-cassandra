package com.datastax.internal.utils;

import org.jetbrains.annotations.Contract;

public class Checks {

    @Contract("null, _ -> fail")
    public static void notNull(final Object argument, final String name)
    {
        if (argument == null)
            throw new IllegalArgumentException(name + " may not be null");
    }

    public static void notNegative(final int n, String name)
    {
        if (n < 0)
            throw new IllegalArgumentException(name + " may not be negative");
    }

    @Contract("false, _ -> fail")
    public static void check(final boolean expression, final String message)
    {
        if (!expression)
            throw new IllegalArgumentException(message);
    }
}
