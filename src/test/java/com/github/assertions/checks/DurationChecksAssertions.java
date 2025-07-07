package com.github.assertions.checks;

import org.junit.jupiter.api.function.ThrowingConsumer;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.github.ChecksHelper.isNegativeError;
import static com.github.ChecksHelper.notPositiveError;
import static com.github.adamorgan.internal.utils.Helpers.durationToString;

public class DurationChecksAssertions extends AbstractChecksAssertions<Duration, DurationChecksAssertions>
{
    public DurationChecksAssertions(String name, ThrowingConsumer<Duration> callable)
    {
        super(name, callable);
    }

    public DurationChecksAssertions checksNotNegative()
    {
        throwsFor(Duration.ofSeconds(-1), isNegativeError(name));
        return this;
    }

    public DurationChecksAssertions checksPositive()
    {
        throwsFor(Duration.ofSeconds(-1), notPositiveError(name));
        throwsFor(Duration.ZERO, notPositiveError(name));
        return this;
    }

    public DurationChecksAssertions checksNotLonger(Duration maxDuration, TimeUnit resolution)
    {
        Duration input = maxDuration.plusSeconds(resolution.toSeconds(1));
        throwsFor(input,
            String.format(Locale.ROOT, "%s may not be longer than %s. Provided: %s",
                name, durationToString(maxDuration, resolution), durationToString(input, resolution)));
        return this;
    }
}
