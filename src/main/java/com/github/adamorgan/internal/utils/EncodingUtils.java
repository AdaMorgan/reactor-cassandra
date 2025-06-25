/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.internal.utils;

import com.github.adamorgan.api.utils.binary.BinaryType;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EncodingUtils
{
    @Nonnull
    public static ByteBuf pack(@Nonnull ByteBuf buffer, @Nullable Serializable value)
    {
        BinaryType type = BinaryType.fromValue(value);
        return pack(buffer, type, value);
    }

    @Nonnull
    public static ByteBuf pack(@Nonnull ByteBuf buffer, @Nonnull BinaryType type, @Nullable Serializable value)
    {
        switch (type)
        {
            case ASCII:
                return EncodingUtils.packUTF88(buffer, value);
            case BIGINT:
                return EncodingUtils.packLong(buffer, value);
            case BLOB:
                return EncodingUtils.packBytes(buffer, value);
            case BOOLEAN:
                return EncodingUtils.packBoolean(buffer, value);
            case COUNTER:
                return EncodingUtils.packLong(buffer, value);
            case DECIMAL:
                return EncodingUtils.packDecimal(buffer, value);
            case DOUBLE:
                return EncodingUtils.packDouble(buffer, value);
            case FLOAT:
                return EncodingUtils.packFloat(buffer, value);
            case INT:
                return EncodingUtils.packInt(buffer, value);
            case TIMESTAMP:
                return EncodingUtils.packDate(buffer, value);
            case UUID:
                return EncodingUtils.packUUID(buffer, value);
            case TEXT:
                return EncodingUtils.packUTF88(buffer, value);
            case VARINT:
                return EncodingUtils.packVarint(buffer, value);
            case TIMEUUID:
                return EncodingUtils.packDate(buffer, value);
            case INET:
                return EncodingUtils.packInet(buffer, value);
            case DATE:
                return EncodingUtils.packDate(buffer, value);
            case TIME:
                return EncodingUtils.packDate(buffer, value);
            case SMALLINT:
                return EncodingUtils.packShort(buffer, value);
            case TINYINT:
                return EncodingUtils.packByte(buffer, value);
            case LIST:
            case MAP:
            case SET:
            case UDT:
            case TUPLE:
            default:
                throw new UnsupportedOperationException(String.format("Unsupported type: 0x%04X", value));
        }
    }

    private static ByteBuf packNull(@Nonnull ByteBuf buffer)
    {
        buffer.writeInt(-1);
        return buffer;
    }

    @Nonnull
    public static ByteBuf packDecimal(@Nonnull ByteBuf buffer, @Nullable Serializable number)
    {
        if (number == null)
        {
            return packNull(buffer);
        }

        try
        {
            BigDecimal decimal = new BigDecimal(number.toString());
            byte[] unscaled = decimal.unscaledValue().toByteArray();
            buffer.writeInt(4 + unscaled.length);
            buffer.writeInt(decimal.scale());
            buffer.writeBytes(unscaled);
            return buffer;
        }
        catch (NumberFormatException failure)
        {
            throw new ClassCastException();
        }
    }

    @Nonnull
    public static ByteBuf packVarint(@Nonnull ByteBuf buffer, @Nullable Serializable number)
    {
        try
        {
            BigInteger integer = new BigInteger(number.toString());
            byte[] bytes = integer.toByteArray();
            buffer.writeInt(bytes.length);
            buffer.writeBytes(bytes);
            return buffer;
        }
        catch (NumberFormatException failure)
        {
            throw new ClassCastException();
        }
    }

    @Nonnull
    public static ByteBuf packDate(@Nonnull ByteBuf buffer, Serializable date)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(date instanceof OffsetDateTime))
        {
            throw new ClassCastException();
        }

        throw new UnsupportedOperationException();
    }

    public static ByteBuf packBytes(@Nonnull ByteBuf buffer, @Nullable Serializable blob)
    {
        if (blob == null)
        {
            return packNull(buffer);
        }

        if (!(blob instanceof byte[]))
        {
            throw new ClassCastException();
        }

        byte[] bytes = (byte[]) blob;

        buffer.writeInt(bytes.length);
        return buffer.writeBytes(bytes);
    }

    public static ByteBuf packInet(@Nonnull ByteBuf buffer, Serializable address)
    {
        if (!(address instanceof InetAddress))
        {
            throw new ClassCastException();
        }

        InetAddress other = (InetAddress) address;
        byte[] addressBytes = other.getAddress();
        buffer.writeByte(addressBytes.length);
        buffer.writeBytes(addressBytes);
        return buffer;
    }

    @Nonnull
    public static ByteBuf packUTF88(@Nonnull ByteBuf buffer, @Nonnull Serializable... obj)
    {
        Checks.notNull(buffer, "Buffer");
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static ByteBuf packUTF88(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        byte[] content = obj.toString().getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(content.length);
        return buffer.writeBytes(content);
    }

    @Nullable
    public static String unpackUTF84(@Nonnull ByteBuf buffer)
    {
        return unpackUTF(buffer, buffer.readUnsignedShort());
    }

    @Nonnull
    public static ByteBuf packUTF84(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        byte[] content = obj.toString().getBytes(StandardCharsets.UTF_8);
        buffer.writeShort(content.length);
        return buffer.writeBytes(content);
    }

    @Nonnull
    public static ByteBuf packLong(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Long))
        {
            throw new ClassCastException();
        }

        long other = (long) obj;

        buffer.writeInt(8);
        return buffer.writeLong(other);
    }

    @Nonnull
    public static ByteBuf packInt(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Integer))
        {
            throw new ClassCastException();
        }

        int other = (int) obj;
        buffer.writeInt(4);
        return buffer.writeInt(other);
    }

    @Nonnull
    public static ByteBuf packByte(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Byte))
        {
            throw new ClassCastException();
        }

        byte other = (byte) obj;
        buffer.writeInt(1);
        return buffer.writeByte(other);
    }

    @Nonnull
    public static ByteBuf packShort(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Short))
        {
            throw new ClassCastException();
        }
        short other = (short) obj;
        return buffer.writeShort(other);
    }

    @Nonnull
    public static ByteBuf packBoolean(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Boolean))
        {
            throw new ClassCastException();
        }

        boolean other = (boolean) obj;
        buffer.writeInt(1);
        return buffer.writeBoolean(other);
    }

    @Nonnull
    public static ByteBuf packDouble(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Double))
        {
            throw new ClassCastException();
        }
        double other = (double) obj;
        buffer.writeInt(4);
        return buffer.writeDouble(other);
    }

    @Nonnull
    public static ByteBuf packFloat(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Float))
        {
            throw new ClassCastException();
        }
        float other = (float) obj;
        buffer.writeInt(4);
        return buffer.writeFloat(other);
    }

    @Nonnull
    public static ByteBuf packUUID(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(obj, "UUID");
        if (!(obj instanceof UUID))
        {
            throw new ClassCastException();
        }

        UUID other = (UUID) obj;
        buffer.writeInt(16);
        buffer.writeLong(other.getMostSignificantBits());
        buffer.writeLong(other.getLeastSignificantBits());
        return buffer;
    }

    @Nonnull
    public static ByteBuf pack(@Nonnull ByteBuf buffer, Map.Entry<String, ? extends Serializable> entry)
    {
        EncodingUtils.packUTF84(buffer, entry.getKey());
        return pack(buffer, entry.getValue());
    }

    @Nullable
    public static OffsetDateTime unpackDate(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        int daysSinceEpoch = buffer.readInt() - Integer.MIN_VALUE;
        LocalDate date = LocalDate.ofEpochDay(daysSinceEpoch);
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    @Nullable
    public static byte[] unpackBytes(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        byte[] result = new byte[length];
        buffer.readBytes(result);
        return result;
    }

    @Nullable
    public static InetAddress unpackInet(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        byte[] addressBytes = new byte[length];
        buffer.readBytes(addressBytes);
        try
        {
            return InetAddress.getByAddress(addressBytes);
        }
        catch (UnknownHostException failException)
        {
            throw new RuntimeException("It was not possible to unpack", failException);
        }
    }

    @Nullable
    public static String unpackUTF(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }

        byte[] content = new byte[length];
        buffer.readBytes(content);
        return new String(content);
    }

    @Nullable
    public static Long unpackLong(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        return buffer.readLong();
    }

    @Nullable
    public static Integer unpackInt(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        return buffer.readInt();
    }

    @Nullable
    public static Short unpackShort(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        return buffer.readShort();
    }

    @Nullable
    public static Boolean unpackBoolean(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        return buffer.readBoolean();
    }

    @Nullable
    public static Double unpackDouble(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        return buffer.readDouble();
    }

    @Nullable
    public static Float unpackFloat(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        return buffer.readFloat();
    }

    @Nullable
    public static UUID unpackUUID(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    @Nullable
    public static <V> Set<V> unpackSet(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        throw new UnsupportedOperationException();
    }

    @Nullable
    public static <V> List<V> unpackList(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        throw new UnsupportedOperationException();
    }

    @Nullable
    public static <K, V> Map<K, V> unpackMap(@Nonnull ByteBuf buffer, int length)
    {
        Checks.notNull(buffer, "Buffer");
        if (length < 0)
        {
            return null;
        }
        throw new UnsupportedOperationException();
    }
}

