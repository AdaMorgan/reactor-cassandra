package com.github.assertions.checks;

import org.junit.jupiter.api.function.ThrowingConsumer;

public class SimpleChecksAssertions<T> extends AbstractChecksAssertions<T, SimpleChecksAssertions<T>>
{
    public SimpleChecksAssertions(String name, ThrowingConsumer<T> callable)
    {
        super(name, callable);
    }
}
