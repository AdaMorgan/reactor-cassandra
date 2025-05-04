package com.github.adamorgan.internal.utils;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class EncodingUtils {

    @Nonnull
    public static ByteBuf encodeUTF88(@Nonnull ByteBuf buffer, @Nonnull Serializable... chars)
    {
        return null;
    }

    public static ByteBuf encodeUTF88(@Nonnull ByteBuf buffer, @Nonnull Serializable chars)
    {
        byte[] content = chars.toString().getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(content.length);
        return buffer.writeBytes(content);
    }

    @Nonnull
    public static String decodeUTF88(@Nonnull ByteBuf buffer)
    {
        int length = buffer.readInt();
        byte[] content = new byte[length];
        buffer.readBytes(content);
        return new String(content);
    }

    public static ByteBuf encodeUTF84(@Nonnull ByteBuf buffer, @Nonnull Serializable... chars)
    {
        return null;
    }

    public static ByteBuf encodeUTF84(@Nonnull ByteBuf buffer, @Nonnull Serializable chars)
    {
        byte[] content = chars.toString().getBytes(StandardCharsets.UTF_8);
        buffer.writeShort(content.length);
        return buffer.writeBytes(content);
    }

    public static ByteBuf encodeLong(@Nonnull ByteBuf buffer, @Nonnull Serializable number)
    {
        buffer.writeInt(Long.BYTES);
        return buffer.writeLong(Long.parseLong(number.toString()));
    }

    public static ByteBuf encodeInt(@Nonnull ByteBuf buffer, @Nonnull Serializable number)
    {
        buffer.writeInt(Integer.BYTES);
        return buffer.writeInt(Integer.parseInt(number.toString()));
    }

    public static ByteBuf encodeBoolean(@Nonnull ByteBuf buffer, @Nonnull Serializable serializable)
    {
        buffer.writeInt(1);
        return buffer.writeBoolean(Boolean.parseBoolean(serializable.toString()));
    }

    @Nonnull
    public static String decodeUTF84(@Nonnull ByteBuf buffer)
    {
        int length = buffer.readUnsignedShort();
        byte[] content = new byte[length];
        buffer.readBytes(content);
        return new String(content);
    }
}
