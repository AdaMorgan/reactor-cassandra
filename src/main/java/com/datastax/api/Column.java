package com.datastax.api;

import java.io.Serializable;

public interface Column
{
    String getName();

    enum Type implements Serializable
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
        INET(0x0010);

        private final int offset;

        <R> Type(int offset)
        {
            this.offset = offset;
        }

        /**
         * Static accessor for retrieving a channel type based on its Discord id key.
         *
         * @param id The id key of the requested channel type.
         * @return The ChannelType that is referred to by the provided key. If the id key is unknown, {@link #UNKNOWN} is returned.
         */
        public static Type fromId(int id)
        {
            for (Type type : values())
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
