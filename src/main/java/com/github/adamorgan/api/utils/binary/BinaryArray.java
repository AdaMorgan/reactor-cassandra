package com.github.adamorgan.api.utils.binary;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import io.netty.buffer.ByteBuf;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * <p>Throws {@link java.lang.IndexOutOfBoundsException}
 * if provided with index out of bounds.
 *
 * <p>This class is not Thread-Safe
 */
public class BinaryArray implements Iterable<BinaryObject>, SerializableArray
{
    public static final BinaryObject[] EMPTY_BUFFER = new BinaryObject[0];

    protected final TLongObjectMap<BinaryObject> elements = new TLongObjectHashMap<>();

    protected final ByteBuf obj;
    protected final int flags;

    public BinaryArray(ByteBuf obj)
    {
        this.obj = obj;
        this.flags = obj.readInt();
    }

    public int getRawFlags()
    {
        return flags;
    }

    public EnumSet<BinaryFlags> getFlags()
    {
        return BinaryFlags.fromBitField(flags);
    }

    public int length()
    {
        return elements.size();
    }

    public boolean isEmpty()
    {
        return elements.isEmpty();
    }

    @Nonnull
    @Override
    public Iterator<BinaryObject> iterator()
    {
        return new ObjectArrayIterator<>(elements.values(EMPTY_BUFFER));
    }

    @Override
    public Spliterator<BinaryObject> spliterator()
    {
        return Spliterators.spliterator(iterator(), length(), Spliterator.IMMUTABLE | Spliterator.NONNULL);
    }

    @Override
    public int hashCode()
    {
        return elements.hashCode();
    }

    @Override
    public void forEach(Consumer<? super BinaryObject> action)
    {
        for (BinaryObject value : this.elements.valueCollection())
        {
            action.accept(value);
        }
    }

    public void clear()
    {
        elements.clear();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }

    @Nonnull
    @Override
    public BinaryArray toBinaryArray()
    {
        return this;
    }
}
