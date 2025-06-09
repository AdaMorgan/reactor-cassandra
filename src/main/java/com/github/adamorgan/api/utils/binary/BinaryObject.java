package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.LibraryLogger;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents an implement Byte Buffer used in communication with the responses Apache Cassandra.
 */
public class BinaryObject implements SerializableBinary, Iterable<Serializable>
{
    public static final Logger LOG = LibraryLogger.getLog(BinaryObject.class);

    protected final AtomicInteger index = new AtomicInteger(0);

    public BinaryObject(ByteBuf buffer)
    {

    }

    public <T extends Serializable> T get(Class<T> clazz, Function<ByteBuf, T> parser)
    {
        return null;
    }

    public boolean getBoolean()
    {
        return false;
    }

    public int getInt()
    {
        return 0;
    }

    public long getLong()
    {
        return 0;
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

    @Nonnull
    @Override
    public Iterator<Serializable> iterator()
    {
        return null;
    }

    @Override
    public Spliterator<Serializable> spliterator()
    {
        return Iterable.super.spliterator();
    }

    @Override
    public void forEach(Consumer<? super Serializable> action)
    {
        Iterable.super.forEach(action);
    }
}
