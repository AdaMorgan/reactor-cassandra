package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.LibraryLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * Represents an implement Byte Buffer used in communication with the responses Apache Cassandra.
 */
public class BinaryObject implements SerializableBinary
{
    public static final Logger LOG = LibraryLogger.getLog(BinaryObject.class);

    protected final ByteBuf obj;

    public BinaryObject(ByteBuf obj)
    {
        this.obj = obj;
    }

    public <T extends Serializable> T get(Class<T> clazz, Function<ByteBuf, T> parser)
    {
        return null;
    }

    public boolean getBoolean()
    {
        return get(Boolean.class, ByteBuf::readBoolean);
    }

    public int getInt()
    {
        return get(Integer.class, ByteBuf::readInt);
    }

    public long getLong()
    {
        return get(Long.class, ByteBuf::readLong);
    }

    public double getDouble()
    {
        return get(Double.class, ByteBuf::readDouble);
    }

    public double getFloat()
    {
        return get(Float.class, ByteBuf::readFloat);
    }

    public UUID getUUID()
    {
        return get(UUID.class, obj -> new UUID(obj.readLong(), obj.readLong()));
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
        return "";
    }
}
