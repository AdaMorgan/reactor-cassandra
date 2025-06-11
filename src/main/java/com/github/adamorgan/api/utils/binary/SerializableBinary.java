package com.github.adamorgan.api.utils.binary;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allows custom serialization for Binary Payloads of an Object.
 */
interface SerializableBinary extends Serializable
{
    /**
     * Serialized {@link BinaryObject BinaryObject} for this object.
     *
     * @return {@link BinaryObject BinaryObject}
     */
    @Nonnull
    BinaryObject toBinary();
}

