package com.github.assertions.checks;

import org.junit.jupiter.api.function.ThrowingConsumer;

public class EnumChecksAssertions<E extends Enum<E>> extends AbstractChecksAssertions<E, EnumChecksAssertions<E>>
{
    public EnumChecksAssertions(String name, ThrowingConsumer<E> callable)
    {
        super(name, callable);
    }

    public EnumChecksAssertions<E> checkIsNot(E variant)
    {
        throwsFor(variant, name + " cannot be " + variant);
        return this;
    }
}
