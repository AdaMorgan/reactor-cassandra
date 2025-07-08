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

package com.github.adamorgan.api.events;

import com.github.adamorgan.internal.utils.Checks;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.EnumSet;

/**
 * An event pushed by the server.
 */
public interface ScheduledEvent
{
    /**
     * The {@link Type type} of the scheduled event.
     *
     * @return The type, or {@link Type#UNKNOWN} if the type is unknown to CQL Binary Protocol.
     */
    @Nonnull
    Type getType();

    /**
     * Represents what type of event an event is, or where the event will be taking place at.
     */
    enum Type
    {
        UNKNOWN(0),

        TOPOLOGY_CHANGE(1),
        STATUS_CHANGE(2),
        SCHEMA_CHANGE(3);
    
        public static final int DEFAULT = 0;
    
        private final int rawValue;
        private final int offset;
    
        Type(int offset)
        {
            this.offset = offset;
            this.rawValue = 1 << offset;
        }
    
        /**
         * The raw bitmask value for this event type
         *
         * @return The raw bitmask value
         */
        public int getRawValue()
        {
            return rawValue;
        }
    
        /**
         * The offset of the event type flag within a bitmask
         * <br>This means {@code getRawValue() == 1 << getOffset()}
         *
         * @return The offset
         */
        public int getOffset()
        {
            return offset;
        }
    
        /**
         * Converts a bitmask into an {@link EnumSet} of enum values.
         *
         * @param raw The raw bitmask
         * @return {@link EnumSet} of types
         */
        @Nonnull
        public static EnumSet<Type> getEventTypes(int raw)
        {
            EnumSet<Type> set = EnumSet.noneOf(Type.class);
            for (Type event : values())
            {
                if ((event.getRawValue() & raw) != 0)
                {
                    set.add(event);
                }
            }
            return set;
        }

        /**
         * Used to retrieve a Status based on a CQL Native Protocol id offset.
         *
         * @param  offset
         *         The CQL Native Protocol id offset representing the requested Status.
         *
         * @return The Status related to the provided offset, or {@link #UNKNOWN Status.UNKNOWN} if the offset is not recognized.
         */
        @Nonnull
        public static ScheduledEvent.Type fromKey(int offset)
        {
            for (Type status : Type.values())
            {
                if (status.offset == offset)
                    return status;
            }

            return UNKNOWN;
        }
    
        /**
         * Converts the given types to a bitmask
         *
         * @param set The {@link Collection} of types
         * @return The bitmask for this set of types
         *
         * @throws IllegalArgumentException If null is provided
         */
        public static int getRaw(@Nonnull Collection<Type> set)
        {
            int raw = 0;
            for (Type event : set)
            {
                raw |= event.rawValue;
            }
            return raw;
        }
    
        /**
         * Converts the given event types to a bitmask
         *
         * @param type The first type
         * @param set The remaining types
         * @return The bitmask for this set of event types
         *
         * @throws IllegalArgumentException If null is provided
         */
        public static int getRaw(@Nonnull Type type, @Nonnull Type... set)
        {
            Checks.notNull(type, "Event Type");
            Checks.notNull(set, "Event Types");
            return getRaw(EnumSet.of(type, set));
        }
    }
}
