/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.internal.utils;

import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.Contract;

import java.util.Collection;

public class Checks
{
    @Contract("null, _ -> fail")
    public static void notNull(final Object argument, final String name)
    {
        if (argument == null)
            throw new IllegalArgumentException(name + " may not be null");
    }

    @Contract("null, _ -> fail")
    public static void notEmpty(final CharSequence argument, final String name)
    {
        notNull(argument, name);
        if (Helpers.isEmpty(argument))
            throw new IllegalArgumentException(name + " may not be empty");
    }

    @Contract("null, _ -> fail")
    public static void notBlank(final CharSequence argument, final String name)
    {
        notNull(argument, name);
        if (Helpers.isBlank(argument))
            throw new IllegalArgumentException(name + " may not be blank");
    }

    @Contract("null, _ -> fail")
    public static void noneNull(final Collection<?> argument, final String name)
    {
        notNull(argument, name);
        argument.forEach(it -> notNull(it, name));
    }

    @Contract("null, _ -> fail")
    public static void noneNull(final Object[] argument, final String name)
    {
        notNull(argument, name);
        for (Object it : argument) {
            notNull(it, name);
        }
    }

    @Contract("false, _, _ -> fail")
    public static void check(final boolean expression, @PrintFormat final String message, final Object... args)
    {
        if (!expression)
            throw new IllegalArgumentException(String.format(message, args));
    }

    public static void positive(final int n, final String name)
    {
        if (n <= 0)
            throw new IllegalArgumentException(name + " may not be negative or zero");
    }

    public static void positive(final long n, final String name)
    {
        if (n <= 0)
            throw new IllegalArgumentException(name + " may not be negative or zero");
    }

    public static void notNegative(final int n, final String name)
    {
        if (n < 0)
            throw new IllegalArgumentException(name + " may not be negative");
    }

    public static void notNegative(final long n, final String name)
    {
        notNegative((int) n, name);
    }

    public static void inRange(final int number, final String name)
    {
        inRange(number, Short.MIN_VALUE, Short.MAX_VALUE, name);
    }

    public static void inRange(final String input, final int min, final int max, final String name)
    {
        notNull(input, name);
        inRange(Helpers.codePointLength(input), min, max, name);
    }

    public static void inRange(final int input, final int min, final int max, final String name)
    {
        check(min <= input && input <= max, "%s must be between %d and %d characters long! Provided: \"%s\"", name, min, max, input);
    }

    public static void notLonger(final String input, final int length, final String name)
    {
        notNull(input, name);
        check(Helpers.codePointLength(input) <= length, "%s may not be longer than %d characters! Provided: \"%s\"", name, length, input);
    }

}
