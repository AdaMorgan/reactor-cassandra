package com.datastax.internal.utils;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class EncodingUtils {

    @Nonnull
    public static ByteBuf encodeMap(@Nonnull ByteBuf buffer, @Nonnull Map<String, String> chars)
    {
        return null;
    }

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

    @Nonnull
    public static String decodeUTF84(@Nonnull ByteBuf buffer)
    {
        int length = buffer.readShort();
        byte[] content = new byte[length];
        buffer.readBytes(content);
        return new String(content);
    }
}
