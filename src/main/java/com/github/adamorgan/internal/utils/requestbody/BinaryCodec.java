package com.github.adamorgan.internal.utils.requestbody;

import io.netty.buffer.ByteBuf;

import java.util.function.BiFunction;

public interface BinaryCodec
{

    enum Codec
    {
        INT(Integer.class, BinaryType.INT::pack),
        BIGINT(Long.class, BinaryType.BIGINT::pack);

        private final Class<?> type;
        private final BiFunction<ByteBuf, ?, ByteBuf> pack;

        <R> Codec(Class<? extends R> type, BiFunction<ByteBuf, ? extends R, ByteBuf> pack)
        {
            this.type = type;
            this.pack = pack;
        }
    }
}
