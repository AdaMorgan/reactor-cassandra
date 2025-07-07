package com.github.assertions.events;

import com.github.adamorgan.internal.LibraryImpl;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

public class EventFiredAssertions<T>
{
    private final Class<T> eventType;
    private final LibraryImpl api;
    private final List<ThrowingConsumer<T>> assertions = new ArrayList<>();

    public EventFiredAssertions(Class<T> eventType, LibraryImpl api)
    {
        this.eventType = eventType;
        this.api = api;
    }

    public <V> EventFiredAssertions<T> hasGetterWithValueEqualTo(Function<T, V> getter, V value)
    {
        assertions.add(event -> assertThat(getter.apply(event)).isEqualTo(value));
        return this;
    }

    public void isFiredBy(Runnable runnable)
    {
        doNothing().when(api).handleEvent(assertArg(arg -> {
            assertThat(arg).isInstanceOf(eventType);
            T casted = eventType.cast(arg);
            for (ThrowingConsumer<T> assertion : assertions)
                assertion.accept(casted);
        }));

        runnable.run();

        verify(api, times(1)).handleEvent(any());
    }
}
