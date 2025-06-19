package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public class BinaryPath
{
    protected final String keyspace, table, name;

    protected final int offset;

    protected final int pack;
    protected final int[] unpack;

    public BinaryPath(ByteBuf raw, String keyspace, String table, String name, int type)
    {
        this.keyspace = keyspace;
        this.table = table;
        this.name = name;
        this.offset = type;
        this.pack = pack(raw, type);
        this.unpack = unpack(pack, type);

        System.out.println("--------------------------");
        System.out.println("0x000" + Integer.toHexString(type));
        System.out.println(pack);
        System.out.println(Arrays.toString(unpack));
        System.out.println("--------------------------");
    }

    public static class BitPacker
    {
        private final int[] bitLengths;
        private final int[] masks;
        private final int[] shifts;

        public BitPacker(int... bitLengths)
        {
            this.bitLengths = bitLengths;
            this.masks = new int[bitLengths.length];
            this.shifts = new int[bitLengths.length];

            int totalBits = 0;
            for (int i = 0; i < bitLengths.length; i++)
            {
                masks[i] = (1 << bitLengths[i]) - 1;
                shifts[i] = totalBits;
                totalBits += bitLengths[i];
                if (totalBits > 32)
                {
                    throw new IllegalArgumentException("Total bits exceed 32");
                }
            }
        }

        public int pack(int... values)
        {
            if (values.length != bitLengths.length)
            {
                throw new IllegalArgumentException("Wrong number of values");
            }

            int packed = 0;
            for (int i = 0; i < values.length; i++)
            {
                if ((values[i] & ~masks[i]) != 0)
                {
                    throw new IllegalArgumentException("Value too large for bit length");
                }
                packed |= (values[i] & masks[i]) << shifts[i];
            }
            return packed;
        }

        public int[] unpack(int packed)
        {
            int[] result = new int[bitLengths.length];
            for (int i = 0; i < result.length; i++)
            {
                result[i] = (packed >> shifts[i]) & masks[i];
            }
            return result;
        }
    }

    private int pack(ByteBuf raw, int type)
    {
        int count;

        switch (type)
        {
            case 0x0020: // LIST
            case 0x0022: // SET
                return new BitPacker(16).pack(raw.readShort());
            case 0x0021: // MAP
                return new BitPacker(16, 16).pack(raw.readShort(), raw.readShort());
            case 0x0030: // UDT
                EncodingUtils.unpackUTF84(raw); // keyspace
                EncodingUtils.unpackUTF84(raw); // name
                count = raw.readUnsignedShort();
                int[] udtValues = new int[count];
                for (int j = 0; j < count; j++)
                {
                    EncodingUtils.unpackUTF84(raw);
                    udtValues[j] = raw.readUnsignedShort();
                }
                return new BitPacker(16).pack(udtValues[0]);

            case 0x0031: // TUPLE
                count = raw.readUnsignedShort();
                int[] tupleValues = new int[count];
                for (int j = 0; j < count; j++)
                {
                    tupleValues[j] = raw.readShort();
                }
                return new BitPacker(16).pack(tupleValues[0]);
            default:
                return 0;
        }
    }

    private int[] unpack(int packed, int type)
    {
        switch (type)
        {
            case 0x0020:
            case 0x0022:
            case 0x0030:
            case 0x0031:
                return new BitPacker(16).unpack(packed);
            case 0x0021:
                return new BitPacker(16, 16).unpack(packed);
            default:
                return new int[0];
        }
    }
}