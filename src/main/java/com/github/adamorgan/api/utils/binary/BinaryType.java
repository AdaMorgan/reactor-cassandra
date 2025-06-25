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

package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.collections.TByteArrayList;
import com.github.adamorgan.internal.utils.collections.TByteHashMap;
import com.github.adamorgan.internal.utils.collections.TByteHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

public enum BinaryType
{
    ASCII(0x0001, String.class),
    BIGINT(0x0002, Long.class),
    BLOB(0x0003, byte[].class),
    BOOLEAN(0x0004, Boolean.class),
    COUNTER(0x0005, Long.class),
    DECIMAL(0x0006, String.class),
    DOUBLE(0x0007, Double.class),
    FLOAT(0x0008, Float.class),
    INT(0x0009, Integer.class),
    TIMESTAMP(0x000B, OffsetDateTime.class, Long.class),
    UUID(0x000C, UUID.class),
    TEXT(0x000D, String.class), // VARCHAR
    VARINT(0x000E, null),
    TIMEUUID(0x000F, UUID.class),
    INET(0x0010, InetAddress.class),
    DATE(0x0011, OffsetDateTime.class),
    TIME(0x0012, Long.class),
    SMALLINT(0x0013, Short.class),
    TINYINT(0x0014, Byte.class),
    LIST(0x0020, TByteArrayList.class),
    MAP(0x0021, TByteHashMap.class),
    SET(0x0022, TByteHashSet.class),
    UDT(0x0030, null),
    TUPLE(0x0031, null);


    private final int offset;
    private final Class<?>[] clazz;

    BinaryType(int offset, Class<? extends Serializable>... clazz)
    {
        this.offset = offset;
        this.clazz = clazz;
    }

    public int getRawValue()
    {
        return offset;
    }

    @Nonnull
    public static <R extends Serializable> BinaryType fromValue(@Nullable final R value)
    {
        return Arrays.stream(values()).filter(type -> type.equals(value)).findFirst().orElseThrow(ClassCastException::new);
    }

    @Nonnull
    public static BinaryType fromValue(int offset)
    {
        return Arrays.stream(values()).filter(type -> type.offset == offset).findFirst().orElseThrow(NullPointerException::new);
    }

    public <R extends Serializable> boolean equals(R obj)
    {
        if (clazz == null)
            return false;
        return Arrays.stream(clazz).anyMatch(clazz -> clazz.isInstance(obj));
    }
}
