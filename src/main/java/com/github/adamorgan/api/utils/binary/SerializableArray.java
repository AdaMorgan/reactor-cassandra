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
