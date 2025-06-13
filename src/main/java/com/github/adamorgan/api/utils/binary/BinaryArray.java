package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * <p>Throws {@link java.lang.IndexOutOfBoundsException} if provided with index out of bounds.
 */
public class BinaryArray implements Iterable<BinaryObject>, SerializableArray
{
    public static final BinaryObject[] EMPTY_BUFFER = new BinaryObject[0];

    protected final List<BinaryObject> elements;

    protected final ByteBuf raw;
    protected final int flags;
    protected final int columnsCount;

    protected final boolean isGlobal;
    protected final boolean hasMorePages;
    protected final boolean hasMetadata;

    protected final String keyspace;
    protected final String table;

    public BinaryArray(@Nonnull ByteBuf raw)
    {
        this.raw = raw;
        this.flags = raw.readInt();

        this.isGlobal = (flags & 0x0001) != 0;
        this.hasMorePages = (flags & 0x0002) != 0;
        this.hasMetadata = (flags & 0x0004) != 0;

        this.columnsCount = raw.readInt();

        this.keyspace = isGlobal ? EncodingUtils.unpackUTF84(raw) : null;
        this.table = isGlobal ? EncodingUtils.unpackUTF84(raw) : null;

        if (hasMorePages)
        {
            raw.skipBytes(raw.readInt()); // The assembly of frames is done via Netty
        }

        this.elements = new BinaryCollector(this).finisher().apply(this).collect(Collectors.toList());

//        if (!hasMetadata)
//        {
//            this.paths = IntStream.range(0, columnsCount)
//                    .mapToObj(i ->
//                    {
//                        String x = !isGlobal ? EncodingUtils.unpackUTF84(raw) : keyspace;
//                        String y = !isGlobal ? EncodingUtils.unpackUTF84(raw) : table;
//
//                        return new BinaryPath(x, y, EncodingUtils.unpackUTF84(raw), raw.readUnsignedShort());
//                    })
//                    .collect(Collectors.toList());
//
//            this.elements = IntStream.range(0, raw.readInt())
//                    .boxed()
//                    .flatMap(i -> this.paths.stream())
//                    .map(path ->
//                    {
//                        int length = raw.readInt();
//                        ByteBuf rawData = raw.readSlice(length).retain();
//                        return new BinaryObject(rawData, path, length);
//                    }).collect(Collectors.toList());
//        }
//        else
//        {
//            throw new UnsupportedOperationException();
//        }
    }

    public int getRawFlags()
    {
        return flags;
    }

    @Nonnull
    public EnumSet<BinaryFlags> getFlags()
    {
        return BinaryFlags.fromBitField(flags);
    }

    public int length()
    {
        return elements.size();
    }

    public boolean isHasMetadata()
    {
        return elements.isEmpty();
    }

    @Nonnull
    @Override
    public Iterator<BinaryObject> iterator()
    {
        return elements.iterator();
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
        for (BinaryObject value : this.elements)
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
        return "";
    }

    @Nonnull
    @Override
    public BinaryArray toBinaryArray()
    {
        return this;
    }
}
