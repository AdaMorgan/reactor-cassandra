package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import java.util.EnumSet;

public class BinaryPath {
    protected final String keyspace, table, name;
    protected final int offset;

    protected final EnumSet<BinaryType> pack;

    public BinaryPath(ByteBuf raw, String keyspace, String table, String name, int type) {
        this.keyspace = keyspace;
        this.table = table;
        this.name = name;
        this.offset = type;

        this.pack = this.pack(raw, BinaryType.fromValue(type));

        System.out.println("--------------------------");
        System.out.println(BinaryType.fromValue(type));
        System.out.println(pack);
        System.out.println("--------------------------");
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