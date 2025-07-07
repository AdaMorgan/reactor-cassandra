package com.github;

import com.github.assertions.checks.*;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.time.Duration;
import java.util.regex.Pattern;

public class ChecksHelper
{
    public static String tooLongError(String name, int maxLength, String value)
    {
        return name + " may not be longer than " + maxLength + " characters! Provided: \"" + value + "\"";
    }

    public static String notInRangeError(String name, int minLength, int maxLength, String value)
    {
        return name + " must be between " + minLength + " and " + maxLength + " characters long! Provided: \"" + value + "\"";
    }

    public static String isNullError(String name)
    {
        return name + " may not be null";
    }

    public static String isEmptyError(String name)
    {
        return name + " may not be empty";
    }

    public static String isBlankError(String name)
    {
        return name + " may not be blank";
    }

    public static String containsWhitespaceError(String name, String argument)
    {
        return name + " may not contain blanks. Provided: \"" + argument + "\"";
    }

    public static String isNotLowercase(String name, String value)
    {
        return name + " must be lowercase only! Provided: \"" + value + "\"";
    }

    public static String notRegexMatch(String name, Pattern pattern, String value)
    {
        return name + " must match regex ^" + pattern + "$. Provided: \"" + value + "\"";
    }

    public static String isNegativeError(String name)
    {
        return name + " may not be negative";
    }

    public static String notPositiveError(String name)
    {
        return name + " may not be negative or zero";
    }

    public static StringChecksAssertions assertStringChecks(String name, ThrowingConsumer<String> callable)
    {
        return new StringChecksAssertions(name, callable);
    }

    public static <E extends Enum<E>> EnumChecksAssertions<E> assertEnumChecks(String name, ThrowingConsumer<E> callable)
    {
        return new EnumChecksAssertions<>(name, callable);
    }

    public static DurationChecksAssertions assertDurationChecks(String name, ThrowingConsumer<Duration> callable)
    {
        return new DurationChecksAssertions(name, callable);
    }

    public static LongChecksAssertions assertLongChecks(String name, ThrowingConsumer<Long> callable)
    {
        return new LongChecksAssertions(name, callable);
    }

    public static <T> SimpleChecksAssertions<T> assertChecks(String name, ThrowingConsumer<T> callable)
    {
        return new SimpleChecksAssertions<>(name, callable);
    }
}
