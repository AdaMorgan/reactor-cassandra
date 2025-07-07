package com.github.assertions.checks;

import org.junit.jupiter.api.function.ThrowingConsumer;

import static com.github.ChecksHelper.notPositiveError;

public class LongChecksAssertions extends AbstractChecksAssertions<Long, LongChecksAssertions>
{
    public LongChecksAssertions(String name, ThrowingConsumer<Long> callable)
    {
        super(name, callable);
    }

    public LongChecksAssertions checksPositive()
    {
        throwsFor( 0L, notPositiveError(name));
        throwsFor( -1L, notPositiveError(name));
        return this;
    }
}
