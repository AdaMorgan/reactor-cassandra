package com.github.adamorgan.internal.utils;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.github.adamorgan.api.utils.binary.BinaryType.*;

public class EncodingUtils {

    @Nullable
    public static Serializable unpack(@Nonnull ByteBuf buffer, int type, int length)
    {
        if (INT.offset == type)
        {
            return EncodingUtils.unpackInt(buffer, length);
        }

        if (BIGINT.offset == type)
        {
            return EncodingUtils.unpackLong(buffer, length);
        }

        if (INET.offset == type)
        {
            return EncodingUtils.unpackInet(buffer, length);
        }

        if (TEXT.offset == type || ASCII.offset == type)
        {
            return EncodingUtils.unpackUTF(buffer, length);
        }

        if (DECIMAL.offset == type)
        {
            return null;
        }

        if (DOUBLE.offset == type)
        {
            return EncodingUtils.unpackDouble(buffer, length);
        }

        if (FLOAT.offset == type)
        {
            return EncodingUtils.unpackFloat(buffer, length);
        }

        if (BLOB.offset == type)
        {
            return EncodingUtils.unpackBytes(buffer, length);
        }

        if (BOOLEAN.offset == type)
        {
            return EncodingUtils.unpackBoolean(buffer, length);
        }

        if (DATE.offset == type)
        {
            return EncodingUtils.unpackDate(buffer, length);
        }

        if (UUID.offset == type)
        {
            return EncodingUtils.unpackUUID(buffer, length);
        }

        throw new UnsupportedOperationException(String.format("Unsupported type: 0x%04X", type));
    }

    public static ByteBuf packList(ByteBuf buffer, Serializable list)
    {
        throw new UnsupportedOperationException();
    }

    public static <R> List<R> unpack(ByteBuf buffer, Class<R> type, int length)
    {
        return null;
    }

    public static ByteBuf packDate(@Nonnull ByteBuf buffer, Serializable date)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(date instanceof OffsetDateTime))
        {
            throw new ClassCastException();
        }

        throw new UnsupportedOperationException();
    }

    public static ByteBuf packBytes(@Nonnull ByteBuf buffer, Serializable blob)
    {
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

        buffer.writeInt(BIGINT.length);
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
        buffer.writeInt(INT.length);
        return buffer.writeInt(other);
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
        buffer.writeShort(SMALLINT.length);
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
        buffer.writeInt(BOOLEAN.length);
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
        buffer.writeInt(DOUBLE.length);
        return buffer.writeDouble(other);
    }

    public static ByteBuf packFloat(@Nonnull ByteBuf buffer, @Nonnull Serializable obj)
    {
        Checks.notNull(buffer, "Buffer");
        if (!(obj instanceof Float))
        {
            throw new ClassCastException();
        }
        float other = (float) obj;
        buffer.writeInt(FLOAT.length);
        return buffer.writeDouble(other);
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
        buffer.writeInt(UUID.length);
        buffer.writeLong(other.getMostSignificantBits());
        buffer.writeLong(other.getLeastSignificantBits());
        return buffer;
    }

    @Nullable
    public static OffsetDateTime unpackDate(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        int daysSinceEpoch = buffer.readInt() - Integer.MIN_VALUE;
        LocalDate date = LocalDate.ofEpochDay(daysSinceEpoch);
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    @Nullable
    public static byte[] unpackBytes(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        byte[] result = new byte[length];
        buffer.readBytes(result);
        return result;
    }

    @Nullable
    public static InetAddress unpackInet(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        byte[] addressBytes = new byte[length];
        buffer.readBytes(addressBytes);
        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException failException) {
            throw new RuntimeException("It was not possible to unpack", failException);
        }
    }

    @Nullable
    public static String unpackUTF(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        byte[] content = new byte[length];
        buffer.readBytes(content);
        return new String(content);
    }

    @Nullable
    public static Long unpackLong(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        return buffer.readLong();
    }

    @Nullable
    public static Integer unpackInt(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        return buffer.readInt();
    }

    @Nullable
    public static Short unpackShort(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        return buffer.readShort();
    }

    @Nullable
    public static Boolean unpackBoolean(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        return buffer.readBoolean();
    }

    @Nullable
    public static Double unpackDouble(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        return buffer.readDouble();
    }

    @Nullable
    public static Float unpackFloat(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        return buffer.readFloat();
    }

    @Nullable
    public static UUID unpackUUID(@Nonnull ByteBuf buffer, int length) {
        Checks.notNull(buffer, "Buffer");
        if (length < 0) {
            return null;
        }
        return new UUID(buffer.readLong(), buffer.readLong());
    }
}
