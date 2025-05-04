package com.github.adamorgan.internal.utils.requestbody;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum BinaryType
{
    BIGINT     (Long.class,      0x0002, 8, EncodingUtils::encodeLong,    ByteBuf::readLong                          ),
    INT        (Integer.class,   0x0009, 4, EncodingUtils::encodeInt,     ByteBuf::readInt                           ),
    TEXT       (String.class,    0x000D, 0, EncodingUtils::encodeUTF88,   EncodingUtils::decodeUTF88                 ),
    VARCHAR    (String.class,    0x000D, 0, EncodingUtils::encodeUTF84,   EncodingUtils::decodeUTF84, false),
    BOOLEAN    (Boolean.class,   0x0004, 1, EncodingUtils::encodeBoolean, ByteBuf::readBoolean                       );

    private final long uid;
    private final int offset;
    private final int length;
    private final BiFunction<ByteBuf, Serializable, ByteBuf> pack;
    private final Function<ByteBuf, Serializable> unpack;
    private final boolean isReadable;

    <T extends Serializable> BinaryType(Class<T> type, int offset, int length, BiFunction<ByteBuf, Serializable, ByteBuf> pack, Function<ByteBuf, Serializable> unpack)
    {
        this(type, ObjectStreamClass::lookup, ObjectStreamClass::getSerialVersionUID, offset, length, pack, unpack, true);
    }

    <T extends Serializable> BinaryType(Class<T> type, int offset, int length, BiFunction<ByteBuf, Serializable, ByteBuf> pack, Function<ByteBuf, Serializable> unpack, boolean isReadable)
    {
        this(type, ObjectStreamClass::lookup, ObjectStreamClass::getSerialVersionUID, offset, length, pack, unpack, isReadable);
    }

    <T extends Serializable> BinaryType(Class<T> type, Function<Class<T>, ObjectStreamClass> lookup, Function<ObjectStreamClass, Long> serialize, int offset, int length, BiFunction<ByteBuf, Serializable, ByteBuf> pack, Function<ByteBuf, Serializable> unpack, boolean isReadable)
    {
        this.offset = offset;
        this.length = length;
        this.pack = pack;
        this.unpack = unpack;
        this.isReadable = isReadable;
        this.uid = lookup.andThen(serialize).apply(type);
    }

    public long getSerialVersionUID()
    {
        return uid;
    }

    public boolean isReadable()
    {
        return isReadable;
    }

    @Nonnull
    public ByteBuf pack(ByteBuf buffer, Serializable value)
    {
        return pack.apply(buffer, value);
    }

    @Nonnull
    public Serializable unpack(ByteBuf buffer)
    {
        return unpack.apply(buffer);
    }

    @Nonnull
    public static ByteBuf pack0(ByteBuf buffer, Serializable value)
    {
        for (BinaryType binaryType : values())
        {
            if (ObjectStreamClass.lookup(value.getClass()).getSerialVersionUID() == binaryType.getSerialVersionUID())
            {
                if (binaryType.isReadable())
                {
                    return binaryType.pack(buffer, value);
                }
            }
        }

        throw new UnsupportedOperationException("Cannot pack value of type " + value.getClass().getName());
    }

    @Nonnull
    public static ByteBuf pack0(ByteBuf buffer, Map.Entry<String, ? extends Serializable> entry)
    {
        BinaryType.VARCHAR.pack(buffer, entry.getKey());
        return pack0(buffer, entry.getValue());
    }
}
