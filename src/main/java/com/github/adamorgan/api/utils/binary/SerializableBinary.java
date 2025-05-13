package com.github.adamorgan.api.utils.binary;

import javax.annotation.Nonnull;

/**
 * Allows custom serialization for Binary Payloads of an Object.
 */
interface SerializableBinary
{
    /**
     * Serialized {@link BinaryObject BinaryObject} for this object.
     *
     * @return {@link BinaryObject BinaryObject}
     */
    @Nonnull
    BinaryObject toBinary();

    /**
     * Serialized {@code ByteArray} for this object.
     * @return a {@code ByteArray} representing the current object
     */
    @Nonnull
    byte[] toByteArray();
}

