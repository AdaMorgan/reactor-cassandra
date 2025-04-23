package com.github.compliance;

import com.github.adamorgan.api.events.Event;
import com.github.adamorgan.api.events.GenericEvent;
import com.github.adamorgan.api.events.UpdateEvent;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class EventConsistencyComplianceTest
{
    static Set<Class<? extends GenericEvent>> eventTypes;
    static Set<Class<? extends GenericEvent>> excludedTypes;

    @BeforeAll
    static void setup()
    {
        Reflections events = new Reflections("com.github.adamorgan.api");
        eventTypes = events.getSubTypesOf(GenericEvent.class);
        excludedTypes = new HashSet<>(Arrays.asList(UpdateEvent.class, Event.class, GenericEvent.class));
    }

    @Test
    void testListenerAdapter()
    {
        Class<ListenerAdapter> adapter = ListenerAdapter.class;
        Set<String> found = new HashSet<>();

        for (Class<? extends GenericEvent> type : eventTypes)
        {
            if (excludedTypes.contains(type))
                continue;
            String name = type.getSimpleName();
            String methodName = "on" + name.substring(0, name.length() - "Event".length());
            assertThatCode(() -> adapter.getDeclaredMethod(methodName, type))
                .as("Method for event " + type + " is missing!")
                .doesNotThrowAnyException();
            found.add(methodName);
        }

        for (Method method : adapter.getDeclaredMethods())
        {
            if (!method.isAccessible() || method.getAnnotation(Deprecated.class) != null)
                continue;
            assertThat(found.contains(method.getName()))
                .as("Dangling method found in ListenerAdapter " + method.getName())
                .isTrue();
        }
    }
}
