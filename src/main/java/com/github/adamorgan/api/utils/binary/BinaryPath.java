package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

public class BinaryPath
{
    protected final String keyspace;
    protected final String table;
    protected final String name;
    protected final int offset;
    protected final List<Integer> types;

    public BinaryPath(ByteBuf raw, String keyspace, String table, String name, int type, BiFunction<ByteBuf, Integer, List<Integer>> types)
    {
        this.keyspace = keyspace;
        this.table = table;
        this.name = name;
        this.offset = type;

        this.types = types.apply(raw, type);
    }

    public static List<Integer> getTypes(ByteBuf raw, int type)
    {
        LinkedList<Integer> types = new LinkedList<>();
        switch (type)
        {
            case 0x0020: //LIST
            case 0x0022: //SET
            {
                int x = raw.readShort();
                types.add(x);
                return types;
            }
            case 0x0021: // MAP
            {
                int x = raw.readShort();
                types.add(x);
                int y = raw.readShort();
                types.add(y);
                return types;
            }
            case 0x0030: //UDT
            {
                String keyspace = EncodingUtils.unpackUTF84(raw);
                String name = EncodingUtils.unpackUTF84(raw);
                int count = raw.readUnsignedShort();
                for (int j = 0; j < count; j++)
                {
                    String fieldName = EncodingUtils.unpackUTF84(raw);
                    int x = raw.readUnsignedShort();
                    types.add(x);
                }
                return types;
            }
            case 0x0031: //TUPLE
            {
                int count = raw.readUnsignedShort();
                for (int j = 0; j < count; j++) {
                    int x = raw.readShort();
                    types.add(x);
                }
                return types;
            }
            default:
            {
                return types;
            }
        }
    }
}
