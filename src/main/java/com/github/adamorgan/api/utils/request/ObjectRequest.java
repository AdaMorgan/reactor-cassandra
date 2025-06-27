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

package com.github.adamorgan.api.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.utils.Checks;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface ObjectRequest<T extends ObjectRequest<T>>
{
    @Nonnull
    String getContent();

    int getFieldsRaw();

    @Nonnull
    EnumSet<ObjectCreateAction.Field> getFields();

    @Nonnull
    ByteBuf getBody();

    int getMaxBufferSize();

    boolean isEmpty();

    @Nonnull
    Consistency getConsistency();

    long getNonce();

    /**
     * Is a flags whose bits define the options for {@link ObjectCreateAction ObjectCreateAction}.
     */
    enum Field
    {
        VALUES(0, Short.BYTES),
        SKIP_METADATA(1, Byte.BYTES),
        PAGE_SIZE(2, Integer.BYTES, true),
        PAGING_STATE(3, Byte.BYTES),
        SERIAL_CONSISTENCY(4, Short.BYTES, true),
        DEFAULT_TIMESTAMP(5, Long.BYTES, true),
        VALUE_NAMES(6, Byte.BYTES);

        /**
         * Bitmask with all fields enabled.
         */
        public static final int ALL_FIELDS = 1 | getRaw(EnumSet.allOf(Field.class));

        /**
         * All fields with some disabled:
         *
         * <ul>
         *     <li>PAGE_SIZE</li>
         *     <li>SERIAL_CONSISTENCY</li>
         *     <li>DEFAULT_TIMESTAMP</li>
         * </ul>
         */
        public static final int DEFAULT = ALL_FIELDS & ~getRaw(VALUES, SKIP_METADATA, PAGING_STATE, VALUE_NAMES, SERIAL_CONSISTENCY);

        /**
         * The number of bytes used to represent a {@link Field Field} value in two's
         * complement binary form.
         *
         * @since 1.8
         */
        public static final int BYTES = 1;

        private final int raw;
        private final int capacity;
        private final boolean isDefault;

        Field(final int offset, final int raw)
        {
            this(offset, raw, false);
        }

        Field(final int offset, final int length, final boolean isDefault)
        {
            this.raw = 1 << offset;
            this.capacity = length;
            this.isDefault = isDefault;
        }

        /**
         * Whether this field is selected by default
         *
         * @return True, if this field is selected by default
         */
        public boolean isDefault()
        {
            return isDefault;
        }

        /**
         * Returns the value of the field as represented in the bitfield.
         *
         * @return Non-Zero bit value of the field
         */
        public int getRawValue()
        {
            return raw;
        }

        /**
         * The raw bit capacity for this Fields
         *
         * @return a bit of capacity
         */
        public int getCapacity()
        {
            return capacity;
        }

        /**
         * Given a bitfield, this function extracts all Enum values according to their bit values and returns
         * an EnumSet containing all matching Fields
         *
         * @param bitfield Non-Negative integer representing a bitfield of Fields
         * @return Never-Null EnumSet of Fields being found in the bitfield
         */
        @Nonnull
        public static EnumSet<Field> fromBitFields(int bitfield)
        {
            Set<Field> set = Arrays.stream(Field.values()).filter(e -> (e.raw & bitfield) > 0).collect(Collectors.toSet());
            return set.isEmpty() ? EnumSet.noneOf(Field.class) : EnumSet.copyOf(set);
        }

        /**
         * Converts the given fields to a bitmask
         *
         * @param set The {@link Collection} of fields
         * @return The bitmask for this set of fields
         */
        public static int getRaw(@Nonnull Collection<Field> set)
        {
            int raw = 0;

            for (Field field : set)
            {
                raw |= field.raw;
            }
            return raw;
        }

        /**
         * Converts the given fields to a bitmask
         *
         * @param field The first field
         * @param set   The remaining fields
         * @return The bitmask for this set of fields
         */
        public static int getRaw(@Nonnull Field field, @Nonnull Field... set)
        {
            Checks.notNull(field, "Field");
            Checks.notNull(field, "Fields");
            return getRaw(EnumSet.of(field, set));
        }

        /**
         * Converts the given fields into raw its bit capacity.
         * This is only useful for set length Buffer initial capacity.
         *
         * @param bitfield Non-Negative integer representing a bitfield of Fields
         * @return The capacity for the provided Fields
         */
        public static int getCapacity(int bitfield)
        {
            return fromBitFields(bitfield).stream().mapToInt(Field::getCapacity).sum();
        }

        /**
         * Converts the given fields into raw its bit capacity.
         * This is only useful for set length Buffer initial capacity.
         *
         * @param fields Non-Negative integer representing a bitfield of Fields
         * @return The capacity for the provided Fields
         */
        public static int getCapacity(@Nonnull EnumSet<Field> fields)
        {
            return fields.stream().mapToInt(Field::getCapacity).sum();
        }
    }

    enum Consistency
    {
        ANY(0x0000),
        ONE(0x0001),
        TWO(0x0002),
        THREE(0x0003),
        QUORUM(0x0004),
        ALL(0x0005),
        LOCAL_QUORUM(0x0006),
        EACH_QUORUM(0x0007),
        SERIAL(0x0008),
        LOCAL_SERIAL(0x0009),
        LOCAL_ONE(0x000A);

        private final short code;

        Consistency(final int code)
        {
            this.code = (short) code;
        }

        public short getCode()
        {
            return this.code;
        }

        /**
         * Whether this consistency level applies to the local data-center only.
         *
         * @return whether this consistency level is {@link #LOCAL_ONE}, {@link #LOCAL_QUORUM}, or {@link #LOCAL_SERIAL}.
         */
        public boolean isLocal()
        {
            return this == LOCAL_ONE || this == LOCAL_QUORUM || this == LOCAL_SERIAL;
        }

        /**
         * Whether or not this consistency level is serial, that is, applies only to the Lightweight transaction
         *
         * <p><b>Example Complete:</b>
         * <pre><code>
         *      INSERT INTO customer_account (username, email)
         *      VALUES (‘user’, ‘user@mail.com’)
         *      IF NOT EXISTS;
         * </code></pre>
         *
         * <p>Serial consistency levels are only meaningful when executing conditional updates ({@code
         * INSERT}, {@code UPDATE} or {@code DELETE} statements with an {@code IF} condition).
         *
         * <p>Two consistency levels belong to this category: {@link #SERIAL} and {@link #LOCAL_SERIAL}.
         *
         * @return whether this consistency level is {@link #SERIAL} or {@link #LOCAL_SERIAL}
         */
        public boolean isSerial()
        {
            return this == SERIAL || this == LOCAL_SERIAL;
        }
    }
}
