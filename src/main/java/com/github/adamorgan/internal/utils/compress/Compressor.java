package com.github.adamorgan.internal.utils.compress;

import com.github.adamorgan.api.utils.Compression;
import io.netty.buffer.ByteBuf;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public interface Compressor<T extends ByteBuf>
{
    /**
     * The name of the algorithm used.
     *
     * <p>It's the algorithm that will be used in the {@code STARTUP} message. {@link Compression#NONE NONE} or empty means no
     * compression.
     */
    @Nonnull
    Compression getType();

    /**
     * Compresses a payload using the "legacy" format of protocol v4- frame bodies.
     *
     * <p>The resulting payload encodes the body length, and is therefore self-sufficient for
     * decompression.
     */
    @Nonnull
    @CheckReturnValue
    T pack(T body);

    /**
     * Decompresses a payload that was body with {@link #pack(ByteBuf)}.
     */
    @Nonnull
    @CheckReturnValue
    T unpack(T body);

    /**
     * Compresses a payload using the "modern" format of protocol v5+ segments.
     *
     * <p>The resulting payload does not encode the body length. It must be stored separately,
     * and provided to the decompression method.
     */
    @Nonnull
    @CheckReturnValue
    T packWithoutLength(T body);

    /** Decompresses a payload that was body with {@link #packWithoutLength(ByteBuf)}. */
    @Nonnull
    T unpackWithoutLength(T body, int length);
}
