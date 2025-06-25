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

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface SerializableArray extends Serializable
{
    @Nonnull
    BinaryArray toBinaryArray();

    enum BinaryFlags
    {
        GLOBAL_TABLE_SPEC(0x0001),
        MORE_PAGES(0x0002),
        METADATA(0x0004);

        public final int code;

        BinaryFlags(int code)
        {
            this.code = code;
        }

        @Nonnull
        public static EnumSet<BinaryFlags> fromBitField(int bitfield)
        {
            Set<BinaryFlags> set = Arrays.stream(BinaryFlags.values())
                    .filter(e -> (e.code & bitfield) > 0)
                    .collect(Collectors.toSet());
            return set.isEmpty() ? EnumSet.noneOf(BinaryFlags.class) : EnumSet.copyOf(set);
        }
    }
}
