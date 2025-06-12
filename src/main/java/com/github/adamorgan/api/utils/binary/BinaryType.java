package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

//TODO-remove: move functional to BinaryObject
public enum BinaryType
{
    ASCII(String.class, 0x0001, 0, EncodingUtils::packUTF84, EncodingUtils::unpackUTF),
    BIGINT(Long.class, 0x0002, 8, EncodingUtils::packLong, EncodingUtils::unpackLong),
    BLOB(byte[].class, 0x0003, 0, EncodingUtils::packBytes, EncodingUtils::unpackBytes),
    DECIMAL(DecimalFormat.class, 0x0006, 0, null, null),
    DOUBLE(Double.class, 0x0007, 8, EncodingUtils::packDouble, EncodingUtils::unpackDouble),
    FLOAT(Float.class, 0x0008, 4, EncodingUtils::packFloat, EncodingUtils::unpackFloat),
    INT(Integer.class, 0x0009, 4, EncodingUtils::packInt, EncodingUtils::unpackInt),
    SMALLINT(Short.class, 0x0013, 2, EncodingUtils::packShort, EncodingUtils::unpackShort),
    TEXT(String.class, 0x000D, 0, EncodingUtils::packUTF88, EncodingUtils::unpackUTF),
    UUID(UUID.class, 0x000C, 16, EncodingUtils::packUUID, EncodingUtils::unpackUUID),
    VARCHAR(String.class, 0x000D, 0, EncodingUtils::packUTF84, EncodingUtils::unpackUTF, false),
    INET(InetAddress.class, 0x0010, 16, EncodingUtils::packInet, EncodingUtils::unpackInet),
    DATE(OffsetDateTime.class, 0x0011, 16, EncodingUtils::packDate, EncodingUtils::unpackDate),
    BOOLEAN(Boolean.class, 0x0004, 1, EncodingUtils::packBoolean, EncodingUtils::unpackBoolean);

    private final long uid;
    public final int offset;
    public final int length;
    private final BiFunction<ByteBuf, Serializable, ByteBuf> pack;
    private final BiFunction<ByteBuf, Integer, Serializable> unpack;
    private final boolean isReadable;

    <T extends Serializable> BinaryType(Class<T> type, int offset, int length, BiFunction<ByteBuf, Serializable, ByteBuf> pack, BiFunction<ByteBuf, Integer, Serializable> unpack)
    {
        this(type, ObjectStreamClass::lookup, ObjectStreamClass::getSerialVersionUID, offset, length, pack, unpack, true);
    }

    <T extends Serializable> BinaryType(Class<T> type, int offset, int length, BiFunction<ByteBuf, Serializable, ByteBuf> pack, BiFunction<ByteBuf, Integer, Serializable> unpack, boolean isReadable)
    {
        this(type, ObjectStreamClass::lookup, ObjectStreamClass::getSerialVersionUID, offset, length, pack, unpack, isReadable);
    }

    <T extends Serializable> BinaryType(Class<T> type, Function<Class<T>, ObjectStreamClass> lookup, Function<ObjectStreamClass, Long> serialize, int offset, int length, BiFunction<ByteBuf, Serializable, ByteBuf> pack, BiFunction<ByteBuf, Integer, Serializable> unpack, boolean isReadable)
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
    public Serializable unpack(ByteBuf buffer, int length)
    {
        return unpack.apply(buffer, length);
    }

    @Nonnull
    public static ByteBuf pack0(ByteBuf buffer, Serializable value)
    {
        for (BinaryType binaryType : values())
        {
            if (ObjectStreamClass.lookup(value.getClass())
                    .getSerialVersionUID() == binaryType.getSerialVersionUID())
            {
                if (binaryType.isReadable())
                {
                    return binaryType.pack(buffer, value);
                }
            }
        }

        throw new UnsupportedOperationException("Cannot pack value of type " + value.getClass()
                .getName());
    }

    @Nonnull
    public static ByteBuf pack0(ByteBuf buffer, Map.Entry<String, ? extends Serializable> entry)
    {
        BinaryType.VARCHAR.pack(buffer, entry.getKey());
        return pack0(buffer, entry.getValue());
    }
}
