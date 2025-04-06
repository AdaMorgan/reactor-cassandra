package com.datastax.api.requests;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public interface ObjectAction<T>
{
    @Nonnull
    ByteBuf applyData();

    enum Level
    {
        ANY(0),
        ONE(1),
        TWO(2),
        THREE(3),
        QUORUM(4),
        ALL(5),
        LOCAL_QUORUM(6),
        EACH_QUORUM(7),
        SERIAL(8),
        LOCAL_SERIAL(9),
        LOCAL_ONE(10);

        private final int code;

        Level(final int code)
        {
            this.code = code;
        }

        public int getCode()
        {
            return code;
        }
    }

    enum Flag
    {
        VALUES(0),
        SKIP_METADATA(1),
        PAGE_SIZE(2),
        PAGING_STATE(3),
        SERIAL_CONSISTENCY(4),
        DEFAULT_TIMESTAMP(5),
        VALUE_NAMES(6);

        private final int value;

        Flag(final int offset)
        {
            this.value = 1 << offset;
        }

        /**
         * Returns the value of the {@link Flag} as represented in the bitfield. It is always a power of 2 (single bit)
         *
         * @return Non-Zero bit value of the field
         */
        public int getValue()
        {
            return value;
        }
    }
}
