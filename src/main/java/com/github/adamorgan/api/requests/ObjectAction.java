package com.github.adamorgan.api.requests;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.requests.action.ObjectActionImpl;
import com.github.adamorgan.internal.requests.action.operator.MapObjectAction;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.request.ObjectData;
import org.jetbrains.annotations.Blocking;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ObjectAction<T>
{
    int HEADER_BYTES = 9;

    @Nonnull
    Library getLibrary();

    int getStreamId();

    @Nonnull
    ObjectData finalizeData();

    default void queue()
    {
        this.queue(null);
    }

    default void queue(@Nullable Consumer<? super T> success)
    {
        this.queue(success, null);
    }

    void queue(@Nullable Consumer<? super T> success, @Nullable Consumer<? super Throwable> failure);

    @Blocking
    default T complete()
    {
        return complete(true);
    }

    @Blocking
    T complete(boolean shouldQueue);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<T> submit(boolean shouldQueue);

    @Nonnull
    @CheckReturnValue
    default CompletableFuture<T> submit()
    {
        return submit(false);
    }

    @Nonnull
    static Consumer<? super Throwable> getDefaultFailure()
    {
        return ObjectActionImpl.getDefaultFailure();
    }

    @Nonnull
    static Consumer<Object> getDefaultSuccess()
    {
        return ObjectActionImpl.getDefaultSuccess();
    }

    @Nonnull
    @CheckReturnValue
    default <O> ObjectAction<O> map(@Nonnull Function<? super T, ? extends O> map)
    {
        Checks.notNull(map, "Function");
        return new MapObjectAction<>(this, map);
    }

    enum Flags
    {
        COMPRESSION(0x01),
        TRACING(0x02),
        CUSTOM_PAYLOAD(0x04),
        WARNING(0x08);

        private final int value;

        Flags(final int offset)
        {
            this.value = offset;
        }

        /**
         * Returns the value of the {@link ObjectCreateAction.Field} as represented in the bitfield. It is always a power of 2 (single bit)
         *
         * @return Non-Zero bit value of the field
         */
        public int getValue()
        {
            return value;
        }

        @Nonnull
        public static EnumSet<Flags> fromBitField(int bitfield)
        {
            Set<Flags> set = Arrays.stream(Flags.values())
                    .filter(e -> (e.value & bitfield) > 0)
                    .collect(Collectors.toSet());
            return set.isEmpty() ? EnumSet.noneOf(Flags.class) : EnumSet.copyOf(set);
        }
    }
}
