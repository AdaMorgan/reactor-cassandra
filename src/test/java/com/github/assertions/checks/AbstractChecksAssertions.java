package com.github.assertions.checks;

import org.junit.jupiter.api.function.ThrowingConsumer;

import static com.github.ChecksHelper.isNullError;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class AbstractChecksAssertions<T, S extends AbstractChecksAssertions<T, S>>
{
    protected final String name;
    protected final ThrowingConsumer<T> callable;

    public AbstractChecksAssertions(String name, ThrowingConsumer<T> callable)
    {
        this.name = name;
        this.callable = callable;
    }

    public S checksNotNull()
    {
        return throwsFor(null, isNullError(name));
    }

    @SuppressWarnings("unchecked")
    public S throwsFor(T input, String expectedError)
    {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> callable.accept(input))
            .withMessage(expectedError);
        return (S) this;
    }
}
