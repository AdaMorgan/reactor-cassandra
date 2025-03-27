package com.datastax.internal.entities;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

public final class EntityBuilder
{
    private final ByteBuf buffer;

    public EntityBuilder()
    {
        this(-1);
    }

    public EntityBuilder(int initialCapacity)
    {
        this.buffer = initialCapacity != -1 ? Unpooled.buffer(initialCapacity) : Unpooled.buffer();
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
        int size = arr.length;

        for (String element : arr)
        {
            byte[] bytes = element.getBytes(CharsetUtil.UTF_8);
            size += bytes.length;
        }

        ByteBuf buffer = Unpooled.buffer(size);

        for (String element : arr)
        {
            byte[] bytes = element.getBytes(CharsetUtil.UTF_8);
            buffer.writeInt(0);
            buffer.writeBytes(bytes);
        }

        return this.writeByte(buffer);
    }

    public EntityBuilder writeEntry(String... arr)
    {
        EntityBuilder builder = new EntityBuilder();

        builder.writeShort(3);

        for (String element : arr)
        {
            builder.writeString(element);
        }

        return this.writeByte(builder);
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
        return this.buffer;
    }

    @Override
    public String toString()
    {
        return ByteBufUtil.prettyHexDump(buffer);
    }
}
