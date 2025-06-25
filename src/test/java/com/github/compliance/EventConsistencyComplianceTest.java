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
