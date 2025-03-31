package com.datastax.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class EntityBuilder implements ByteBufConvertible
{
    private final ByteBuf buffer;

    public EntityBuilder()
    {
        this.buffer = Unpooled.buffer();
    }

    public EntityBuilder(int initialCapacity)
    {
        this.buffer = Unpooled.buffer(initialCapacity);
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
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        this.buffer.writeInt(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public EntityBuilder writeString(String... arr)
    {
        EntityBuilder collect = Arrays.stream(arr).collect(EntityBuilder::new, EntityBuilder::writeString, EntityBuilder::writeByte);
        return this.writeByte(collect);
    }

    public EntityBuilder writeByte(EntityBuilder buffer)
    {
        this.writeByte(buffer.buffer);
        return this;
    }

    public EntityBuilder writeByte(ByteBuf buffer)
    {
        this.buffer.writeInt(buffer.readableBytes());
        this.buffer.writeBytes(buffer);
        return this;
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
        this.buffer.writeBytes(bytes);
        return this;
    }

    @Override
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
