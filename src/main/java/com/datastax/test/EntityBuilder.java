package com.datastax.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class EntityBuilder implements ByteBufConvertible
{
    protected final ByteBuf buffer;

    public EntityBuilder()
    {
        this.buffer = Unpooled.buffer();
    }

    public EntityBuilder(int initialCapacity)
    {
        this.buffer = Unpooled.buffer(initialCapacity);
    }

    public enum TypeTag
    {
        INT(1, ByteBuf::writeInt),
        SHORT(2, ByteBuf::writeShort),
        BYTE(3, ByteBuf::writeByte),
        ;

        private final int offset;
        private final BiConsumer<ByteBuf, Integer> handler;

        TypeTag(int offset, BiConsumer<ByteBuf, Integer> handler)
        {
            this.offset = offset;
            this.handler = handler;
        }

        public void writeLock(ByteBuf buffer, int value)
        {
            handler.accept(buffer, value);
        }
    }

    public EntityBuilder writeLock(int value, TypeTag tag)
    {
        tag.writeLock(this.buffer, value);
        return this;
    }

    public EntityBuilder writeByte(int value)
    {
        this.writeLock(value, TypeTag.BYTE);
        return this;
    }

    public EntityBuilder writeInt(int value)
    {
        this.writeLock(value, TypeTag.INT);
        return this;
    }

    public EntityBuilder writeShort(int value)
    {
        this.writeLock(value, TypeTag.SHORT);
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
        EntityBuilder collect = Arrays.stream(arr).collect(EntityBuilder::new, EntityBuilder::writeString, EntityBuilder::writeBytes);
        return this.writeBytes(collect);
    }

    public EntityBuilder writeBytes(EntityBuilder buffer)
    {
        this.writeBytes(buffer.buffer);
        return this;
    }

    public EntityBuilder writeBytes(ByteBuf buffer)
    {
        this.buffer.writeInt(buffer.readableBytes());
        this.buffer.writeBytes(buffer);
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
