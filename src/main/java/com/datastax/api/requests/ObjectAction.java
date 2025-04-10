package com.datastax.api.requests;

import com.datastax.api.Library;
import io.netty.buffer.ByteBuf;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ObjectAction<T>
{
    @Nonnull
    Library getLibrary();

    @Nonnull
    ByteBuf applyData();

    default void queue()
    {
        this.queue(null);
    }

    default void queue(@Nullable Consumer<? super T> success)
    {
        this.queue(success, null);
    }

    void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<T> submit(boolean shouldQueue);

    @Nonnull
    @CheckReturnValue
    default CompletableFuture<T> submit()
    {
        return submit(false);
    }

    default void as()
    {
        throw new UnsupportedOperationException();
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
