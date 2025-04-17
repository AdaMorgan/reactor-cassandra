package com.datastax.api.requests.objectaction;

import com.datastax.api.requests.ObjectAction;
import com.datastax.api.utils.request.ObjectCreateRequest;
import io.netty.buffer.ByteBuf;

public interface ObjectCreateAction extends ObjectAction<ByteBuf>, ObjectCreateRequest<ObjectCreateAction>
{
    enum ObjectFlags
    {
        VALUES(0),
        SKIP_METADATA(1),
        PAGE_SIZE(2),
        PAGING_STATE(3),
        SERIAL_CONSISTENCY(4),
        DEFAULT_TIMESTAMP(5),
        VALUE_NAMES(6);

        private final int value;

        ObjectFlags(final int offset)
        {
            this.value = 1 << offset;
        }

        /**
         * Returns the value of the {@link ObjectFlags} as represented in the bitfield. It is always a power of 2 (single bit)
         *
         * @return Non-Zero bit value of the field
         */
        public int getValue()
        {
            return value;
        }
    }

    enum Consistency
    {
        ANY(0x0000),
        ONE(0x0001),
        TWO(0x0002),
        THREE(0x0003),
        QUORUM(0x0004),
        ALL(0x0005),
        LOCAL_QUORUM(0x0006),
        EACH_QUORUM(0x0007),
        SERIAL(0x0008),
        LOCAL_SERIAL(0x0009),
        LOCAL_ONE(0x000A);

        public final short code;

        Consistency(final int code)
        {
            this.code = (short) code;
        }

        public int getCode()
        {
            return this.code;
        }
    }
}
