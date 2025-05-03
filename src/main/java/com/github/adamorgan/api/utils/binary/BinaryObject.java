package com.github.adamorgan.api.utils.binary;

import javax.annotation.Nonnull;

public class BinaryObject implements SerializableBinary
{
    @Nonnull
    @Override
    public BinaryObject toBinary()
    {
        return this;
    }

    @Nonnull
    @Override
    public byte[] toByteArray()
    {
        return new byte[0];
    }
}
