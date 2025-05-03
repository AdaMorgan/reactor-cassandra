package com.github.adamorgan.internal.utils;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EncodingUtils {

    @Nonnull
    public static ByteBuf encodeUTF88(@Nonnull ByteBuf buffer, @Nonnull String... chars)
    {
        return null;
    }

    public static ByteBuf encodeUTF88(@Nonnull ByteBuf buffer, @Nonnull String chars)
    {
        byte[] content = chars.getBytes(StandardCharsets.UTF_8);
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

    public static ByteBuf encodeUTF84(@Nonnull ByteBuf buffer, @Nonnull String... chars)
    {
        return null;
    }

    public static ByteBuf encodeUTF84(@Nonnull ByteBuf buffer, @Nonnull String chars)
    {
        byte[] content = chars.getBytes(StandardCharsets.UTF_8);
        buffer.writeShort(content.length);
        return buffer.writeBytes(content);
    }

    public static ByteBuf encodeLong(@Nonnull ByteBuf buffer, long number)
    {
        buffer.writeInt(8);
        return buffer.writeLong(number);
    }

    public static ByteBuf encodeInt(@Nonnull ByteBuf buffer, int number)
    {
        buffer.writeInt(4);
        return buffer.writeInt(number);
    }

    @Nonnull
    public static String decodeUTF84(@Nonnull ByteBuf buffer)
    {
        int length = buffer.readShort();
        byte[] content = new byte[length];
        buffer.readBytes(content);
        return new String(content);
    }
}
