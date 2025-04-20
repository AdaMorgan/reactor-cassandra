package com.datastax.api.requests.objectaction;

import com.datastax.api.requests.ObjectAction;
import com.datastax.api.utils.request.ObjectCreateRequest;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

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

        private final short code;

        Consistency(final int code)
        {
            this.code = (short) code;
        }

        public short getCode()
        {
            return this.code;
        }

        /**
         * Whether this consistency level applies to the local data-center only.
         *
         * @return whether this consistency level is {@link #LOCAL_ONE}, {@link #LOCAL_QUORUM}, or {@link #LOCAL_SERIAL}.
         */
        public boolean isLocal()
        {
            return this == LOCAL_ONE || this == LOCAL_QUORUM || this == LOCAL_SERIAL;
        }

        /**
         * Whether or not this consistency level is serial, that is, applies only to the Lightweight transaction
         *
         * <p><b>Example Complete:</b>
         * <pre><code>
         *      INSERT INTO customer_account (username, email)
         *      VALUES (‘user’, ‘user@mail.com’)
         *      IF NOT EXISTS;
         * </code></pre>
         *
         * <p>Serial consistency levels are only meaningful when executing conditional updates ({@code
         * INSERT}, {@code UPDATE} or {@code DELETE} statements with an {@code IF} condition).
         *
         * <p>Two consistency levels belong to this category: {@link #SERIAL} and {@link #LOCAL_SERIAL}.
         *
         * @return whether this consistency level is {@link #SERIAL} or {@link #LOCAL_SERIAL}
         */
        public boolean isSerial()
        {
            return this == SERIAL || this == LOCAL_SERIAL;
        }
    }
}
