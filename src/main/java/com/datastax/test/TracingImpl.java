package com.datastax.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.*;
import java.util.function.Function;

public class TracingImpl
{
    public ByteBuf read(byte flags, ByteBuf buf, Function<ByteBuf, ByteBuf> handler)
    {
        UUID tracingId;
        List<String> warnings;
        Map<String, ByteBuf> customPayload;

        if ((flags & 0x02) != 0) {
            tracingId = readUUID(buf);
            System.out.println("TRACING ID: " + tracingId);
        }

        if ((flags & 0x08) != 0) {
            warnings = readStringList(buf);
            System.out.println("WARNINGS: " + warnings);
        }

        if ((flags & 0x04) != 0) {
            customPayload = readCustomPayload(buf);
            System.out.println("CUSTOM PAYLOAD: " + customPayload);
        }

        return handler.apply(buf);
    }

    private static UUID readUUID(ByteBuf byteBuf) {
        long mostSigBits = byteBuf.readLong();
        long leastSigBits = byteBuf.readLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    private static List<String> readStringList(ByteBuf byteBuf) {
        int count = byteBuf.readUnsignedShort();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(readString(byteBuf));
        }
        return list;
    }

    private static Map<String, ByteBuf> readCustomPayload(ByteBuf byteBuf) {
        int count = byteBuf.readUnsignedShort();
        Map<String, ByteBuf> payload = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String key = readString(byteBuf);
            ByteBuf value = readBytes(byteBuf);
            payload.put(key, value);
        }
        return payload;
    }

    private static String readString(ByteBuf byteBuf) {
        int length = byteBuf.readUnsignedShort();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        return new String(bytes);
    }

    private static ByteBuf readBytes(ByteBuf byteBuf) {
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        return Unpooled.wrappedBuffer(bytes);
    }
}
