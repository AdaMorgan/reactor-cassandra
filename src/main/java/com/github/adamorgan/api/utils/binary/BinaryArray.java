package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import io.netty.buffer.ByteBuf;
import org.apache.commons.collections4.iterators.ObjectArrayIterator;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

/**
 * <p>Throws {@link java.lang.IndexOutOfBoundsException} if provided with index out of bounds.
 */
public class BinaryArray implements Iterable<BinaryObject>, SerializableArray
{
    public static final BinaryObject[] EMPTY_BUFFER = new BinaryObject[0];

    protected final TLongObjectMap<BinaryObject> elements = new TLongObjectHashMap<>();

    protected final ByteBuf obj;
    protected final int flags;

    public BinaryArray(@Nonnull ByteBuf obj)
    {
        this.obj = obj;
        this.flags = obj.readInt();

        boolean hasGlobalTableSpec = (flags & 0x0001) != 0;
        boolean hasMorePages = (flags & 0x0002) != 0;
        boolean noMetadata = (flags & 0x0004) != 0;

        String keyspace = "";
        String table = "";

        int columnsCount = obj.readInt();

        if (hasGlobalTableSpec)
        {
            keyspace = EncodingUtils.unpackUTF84(obj);
            table = EncodingUtils.unpackUTF84(obj);
        }

        if (hasMorePages)
        {
            obj.skipBytes(obj.readInt());
        }

        List<ColumnImpl> columns = new ArrayList<>();
        if (!noMetadata)
        {
            for (int i = 0; i < columnsCount; i++)
            {
                if (!hasGlobalTableSpec)
                {
                    keyspace = EncodingUtils.unpackUTF84(obj);
                    table = EncodingUtils.unpackUTF84(obj);
                }

                String name = EncodingUtils.unpackUTF84(obj);
                int type = obj.readUnsignedShort();

                switch (type)
                {
                    case 0x0020: // LIST
                    case 0x0022: // SET
                    {
                        int x = obj.readShort();
                        break;
                    }
                    case 0x0021: // MAP
                    {
                        int k = obj.readShort();
                        int v = obj.readShort();
                        break;
                    }
                    case 0x0030: //UDP
                    {
                        String udtKeyspace = EncodingUtils.unpackUTF84(obj);
                        String udtName = EncodingUtils.unpackUTF84(obj);
                        int fieldsCount = obj.readUnsignedShort();
                        for (int j = 0; j < fieldsCount; j++) {
                            String fieldName = EncodingUtils.unpackUTF84(obj);
                            int x = obj.readUnsignedShort();
                        }
                        break;
                    }
                    case 0x0031: // TUPLE
                    {
                        int count = obj.readUnsignedShort();
                        for (int j = 0; j < count; j++) {
                            int x = obj.readShort();
                        }
                        break;
                    }
                }

                ColumnImpl column = new ColumnImpl(keyspace, table, name, type);
                columns.add(column);
            }

            int rowsCount = obj.readInt();

            List<Serializable> rows = new ArrayList<>();
            for (int rowNumber = 0; rowNumber < rowsCount; rowNumber++)
            {
                for (ColumnImpl column : columns)
                {
                    int length = obj.readInt();
                    Serializable value = EncodingUtils.unpack(obj, column.type, length);
                    rows.add(value);
                }
            }
            System.out.println(rows);
        }
    }

    public static class ColumnImpl
    {
        private final String keyspace;
        private final String tableName;
        private final String name;
        private final int type;

        public ColumnImpl(String keyspace, String tableName, String name, int type)
        {
            this.keyspace = keyspace;
            this.tableName = tableName;
            this.name = name;
            this.type = type;
        }

        @Nonnull
        public String getName()
        {
            return name;
        }

        public int getType()
        {
            return type;
        }

        @Override
        public String toString()
        {
            return String.format("%s.%s.%s [%s]", keyspace, tableName, name, type);
        }
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
        return "";
    }

    @Nonnull
    @Override
    public BinaryArray toBinaryArray()
    {
        return this;
    }
}
