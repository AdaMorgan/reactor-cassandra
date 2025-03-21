package com.datastax.internal.entities;

import com.datastax.test.SocketClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

public final class EntityBuilder
{
    private final ByteBuf buffer;

    public EntityBuilder()
    {
        this.buffer = Unpooled.buffer();
    }

    public EntityBuilder writeByte(byte value)
    {
        this.buffer.writeByte(value);
        return this;
    }

    public EntityBuilder writeByte(int value)
    {
        this.buffer.writeByte(value);
        return this;
    }

    public EntityBuilder writeBytes(byte[] bytes)
    {
        this.buffer.writeInt(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public EntityBuilder writeInt(int value)
    {
        this.buffer.writeInt(value);
        return this;
    }

    public EntityBuilder writeShort(short value)
    {
        this.buffer.writeShort(value);
        return this;
    }

    public EntityBuilder writeShort(int value)
    {
        this.buffer.writeShort(value);
        return this;
    }

    public EntityBuilder writeString(String str)
    {
        byte[] bytes = str.getBytes(CharsetUtil.UTF_8);
        this.buffer.writeShort(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public EntityBuilder writeString(String... arr)
    {
        EntityBuilder entityBuilder = new EntityBuilder().writeInt(arr.length);

        for (String element : arr)
        {
            entityBuilder.writeString(element);
        }

        this.writeByte(entityBuilder.buffer);
        return this;
    }

    public EntityBuilder writeStringMap(Map<String, String> m)
    {
        ByteBuf body = Unpooled.buffer();
        SocketClient.Writer.writeStringMap(m, body);
        this.writeByte(body);
        return this;
    }

    public EntityBuilder writeStringEntry(String key, String value)
    {
        this.writeInt(2);
        this.writeString(key);
        this.writeString(value);
        return this;
    }

    public EntityBuilder writeLongString(String str)
    {
        byte[] bytes = str.getBytes(CharsetUtil.UTF_8);
        this.buffer.writeInt(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public EntityBuilder writeByte(EntityBuilder builder)
    {
        return writeByte(builder.buffer);
    }

    public EntityBuilder writeByte(ByteBuf buffer)
    {
        this.buffer.writeInt(buffer.readableBytes());
        this.buffer.writeBytes(buffer);
        return this;
    }

    public ByteBuf asByteBuf()
    {
        return buffer;
    }

    @Override
    public String toString()
    {
        return ByteBufUtil.prettyHexDump(buffer);
    }
}
