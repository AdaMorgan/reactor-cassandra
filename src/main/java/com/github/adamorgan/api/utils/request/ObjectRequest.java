package com.github.adamorgan.api.utils.request;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public interface ObjectRequest<T extends ObjectRequest<T>>
{
    @Nonnull
    String getContent();

    int getFieldsRaw();

    @Nonnull
    EnumSet<ObjectCreateAction.Field> getFields();

    @Nonnull
    ByteBuf getBody();

    int getMaxBufferSize();

    boolean isEmpty();

    @Nonnull
    Consistency getConsistency();

    long getNonce();

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
