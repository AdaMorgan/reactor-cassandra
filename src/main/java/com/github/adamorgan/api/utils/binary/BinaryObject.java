package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import com.github.adamorgan.internal.utils.LibraryLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Represents an implement Byte Buffer used in communication with the responses Apache Cassandra.
 */
public class BinaryObject implements SerializableBinary
{
    public static final Logger LOG = LibraryLogger.getLog(BinaryObject.class);

    private final ByteBuf obj;
    private final BinaryPath path;
    private final int raw;
    private final int length;

    public BinaryObject(@Nonnull ByteBuf obj, final BinaryPath path, final int length)
    {
        this.obj = obj;
        this.path = path;
        this.raw = 1 << path.offset;
        this.length = length;
    }

    public int getRawValue()
    {
        return raw;
    }

    @Nullable
    public <T> T get(@Nonnull Class<T> clazz, @Nonnull BiFunction<ByteBuf, Integer, T> parser)
    {
        return this.length >= 0 ? parser.apply(obj, length) : null;
    }

    @Nullable
    public String getString()
    {
        return get(String.class, EncodingUtils::unpackUTF);
    }

    public boolean getBoolean()
    {
        return get(Boolean.class, EncodingUtils::unpackBoolean);
    }

    public int getInt()
    {
        return get(Integer.class, EncodingUtils::unpackInt);
    }

    public long getLong()
    {
        return get(Long.class, EncodingUtils::unpackLong);
    }

    public double getDouble()
    {
        return get(Double.class, EncodingUtils::unpackDouble);
    }

    public double getFloat()
    {
        return get(Float.class, EncodingUtils::unpackFloat);
    }

    @Nonnull
    public UUID getUUID()
    {
        return get(UUID.class, EncodingUtils::unpackUUID);
    }

    @Nonnull
    public <V> Set<V> getSet()
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public <V> List<V> getList()
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public <K, V> Map<K, V> getMap()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof BinaryObject))
            return false;
        if (obj == this)
            return true;
        BinaryObject other = (BinaryObject) obj;
        return ByteBufUtil.equals(this.obj, other.obj);
    }

    @Nonnull
    @Override
    public BinaryObject toBinary()
    {
        return this;
    }

    @Override
    public String toString()
    {
        return ByteBufUtil.prettyHexDump(obj);
    }
}
