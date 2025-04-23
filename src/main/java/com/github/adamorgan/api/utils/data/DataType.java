package com.github.adamorgan.api.utils.data;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Function;

public class DataType<T> implements TypeCodec<T>
{
    public static final DataType<Long>    BIGINT     = new DataType<>(0x0002, 8, ByteBuf::writeLong,    ByteBuf::readLong);
    public static final DataType<Boolean> BOOLEAN    = new DataType<>(0x0004, 1, ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final DataType<Integer> INT        = new DataType<>(0x0009, 4, ByteBuf::writeInt,     ByteBuf::readInt);

    public static final DataType<String> STRING      = new DataType<>(0x000D, 0, EncodingUtils::encodeUTF84, EncodingUtils::decodeUTF84);
    public static final DataType<String> LONG_STRING = new DataType<>(0x000D, 0, EncodingUtils::encodeUTF88, EncodingUtils::decodeUTF88);

    private final int offset, length;
    private final BiFunction<ByteBuf, T, ByteBuf> encode;
    private final Function<ByteBuf, T> decode;

    public DataType(int offset, int length, BiFunction<ByteBuf, T, ByteBuf> encode, Function<ByteBuf, T> decode)
    {
        this.offset = offset;
        this.length = length;
        this.encode = encode;
        this.decode = decode;
    }

    @Nonnull
    @Override
    public ByteBuf encode(ByteBuf buffer, T value)
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
