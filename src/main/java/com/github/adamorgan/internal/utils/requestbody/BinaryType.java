package com.github.adamorgan.internal.utils.requestbody;

import com.github.adamorgan.internal.utils.EncodingUtils;
import com.github.adamorgan.internal.utils.codec.TypeCodec;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BinaryType<T> implements TypeCodec<T>
{
    public static final BinaryType<Long>    BIGINT     = new BinaryType<>(0x0002, 8, EncodingUtils::encodeLong,    ByteBuf::readLong);
    public static final BinaryType<Integer> INT        = new BinaryType<>(0x0009, 4, EncodingUtils::encodeInt,     ByteBuf::readInt);

    public static final BinaryType<String> STRING      = new BinaryType<>(0x000D, 0, EncodingUtils::encodeUTF84, EncodingUtils::decodeUTF84);
    public static final BinaryType<String> LONG_STRING = new BinaryType<>(0x000D, 0, EncodingUtils::encodeUTF88, EncodingUtils::decodeUTF88);

    public static final BinaryType<Boolean> BOOLEAN    = new BinaryType<>(0x0004, 1, ByteBuf::writeBoolean, ByteBuf::readBoolean);

    private final int offset, length;
    private final BiFunction<ByteBuf, T, ByteBuf> encode;
    private final Function<ByteBuf, T> decode;

    public BinaryType(int offset, int length, BiFunction<ByteBuf, T, ByteBuf> encode, Function<ByteBuf, T> decode)
    {
        this.offset = offset;
        this.length = length;
        this.encode = encode;
        this.decode = decode;
    }

    @Nonnull
    @Override
    public ByteBuf pack(ByteBuf buffer, T value)
    {
        return encode.apply(buffer, value);
    }

    @Nonnull
    @Override
    public T decode(ByteBuf buffer)
    {
        return decode.apply(buffer);
    }
}
