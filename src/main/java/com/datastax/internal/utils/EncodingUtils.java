package com.datastax.internal.utils;

import com.datastax.api.CustomInfo;
import io.netty.buffer.ByteBuf;

public final class EncodingUtils
{
    private EncodingUtils() { }

    public static <R> R encode(ByteBuf buffer, Class<R> clazz)
    {
        return null;
    }

    public enum DataType
    {
        UNKNOWN(0x0000),

        ASCII(0x0001),
        BIGINT(0x0002),
        BLOB(0x0003),
        BOOLEAN(0x0004),
        COUNTER(0x0005),
        DECIMAL(0x0006),
        DOUBLE(0x0007),
        FLOAT(0x0008),
        INT(0x0009),
        TIMESTAMP(0x000B),
        UUID(0x000C),
        VARCHAR(0x000D),
        VARINT(0x000E),
        TIMEUUID(0x000F),
        INET(0x0010),
        DATE(0x0011),
        TIME(0x0012),
        SLALLINT(0x0013),
        TANYINT(0x0014),
        LIST(0x0020),
        MAP(0x0021),
        SET(0x0022);

        private final int offset;

        DataType(int offset)
        {
            this.offset = offset;
        }

        /**
         * Static accessor for retrieving a channel type based on its {@value CustomInfo#PROJECT_NAME } id key.
         *
         * @param id The id key of the requested channel type.
         * @return The ChannelType that is referred to by the provided key. If the id key is unknown, {@link #UNKNOWN} is returned.
         */
        public static DataType fromId(int id)
        {
            for (DataType type : values())
            {
                if (type.offset == id)
                {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

}
