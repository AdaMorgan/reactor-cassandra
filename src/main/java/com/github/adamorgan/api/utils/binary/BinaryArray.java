/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BinaryArray implements Iterable<BinaryObject>, SerializableArray
{
    public static final BinaryObject[] EMPTY_BUFFER = new BinaryObject[0];

    private final List<BinaryObject> elements;
    private final ByteBuf raw;
    private final int flags;
    private final int columnsCount;
    private final boolean isGlobal;
    private final boolean hasMorePages;
    private final boolean hasMetadata;
    private final String keyspace;
    private final String table;

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
            raw.skipBytes(raw.readInt());
        }

        this.elements = parseElements();
    }

    private List<BinaryObject> parseElements()
    {
        List<Path> paths = createPaths();
        int objectCount = raw.readInt();

        List<BinaryObject> result = new ArrayList<>(objectCount);
        for (int i = 0; i < objectCount; i++)
        {
            for (Path path : paths)
            {
                result.add(createBinaryObject(path));
            }
        }
        return result;
    }

    private List<Path> createPaths()
    {
        return IntStream.range(0, columnsCount).mapToObj(this::createPath).collect(Collectors.toList());
    }

    private Path createPath(int index)
    {
        String pathKeyspace = isGlobal ? keyspace : EncodingUtils.unpackUTF84(raw);
        String pathTable = isGlobal ? table : EncodingUtils.unpackUTF84(raw);
        String name = EncodingUtils.unpackUTF84(raw);
        int type = raw.readUnsignedShort();
        return new Path(raw, pathKeyspace, pathTable, name, type);
    }

    private BinaryObject createBinaryObject(Path path)
    {
        int length = raw.readInt();
        ByteBuf rawData = length >= 0 ? raw.readSlice(length).retain() : Unpooled.EMPTY_BUFFER;
        return new BinaryObject(rawData, path, length);
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

    @Nonnull
    @Override
    public Spliterator<BinaryObject> spliterator()
    {
        return Spliterators.spliterator(iterator(), length(), Spliterator.IMMUTABLE | Spliterator.NONNULL);
    }

    public int hashCode()
    {
        return elements.hashCode();
    }

    public void forEach(Consumer<? super BinaryObject> action)
    {
        elements.forEach(action);
    }

    public void clear()
    {
        elements.clear();
    }

    @Nonnull
    public BinaryArray toBinaryArray()
    {
        return this;
    }

    public static class Path
    {
        protected final String keyspace, table, name;
        protected final int offset;

        protected final EnumSet<BinaryType> pack;

        public Path(ByteBuf raw, String keyspace, String table, String name, int type) {
            this.keyspace = keyspace;
            this.table = table;
            this.name = name;
            this.offset = type;

            this.pack = this.pack(raw, BinaryType.fromValue(type));
        }

        private EnumSet<BinaryType> pack(ByteBuf raw, BinaryType type)
        {
            EnumSet<BinaryType> types = EnumSet.noneOf(BinaryType.class);
            switch (type)
            {
                case LIST:
                case SET:
                    return EnumSet.of(BinaryType.fromValue(raw.readShort()));
                case MAP:
                    return EnumSet.of(BinaryType.fromValue(raw.readShort()), BinaryType.fromValue(raw.readShort()));
                case UDT:
                    EncodingUtils.unpackUTF84(raw); // keyspace
                    EncodingUtils.unpackUTF84(raw); // name
                    int udtFieldCount = raw.readUnsignedShort();
                    for (int i = 0; i < udtFieldCount; i++)
                    {
                        EncodingUtils.unpackUTF84(raw); // field name
                        types.add(BinaryType.fromValue(raw.readUnsignedShort()));
                    }
                    return types;
                case TUPLE:
                    int tupleCount = raw.readUnsignedShort();
                    for (int i = 0; i < tupleCount; i++)
                    {
                        types.add(BinaryType.fromValue(raw.readShort() & 0xFFFF));
                    }
                    return types;
                default:
                    return types;
            }
        }
    }
}