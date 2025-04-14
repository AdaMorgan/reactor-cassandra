package com.datastax.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
        BYTE(1, ByteBuf::writeByte),
        SHORT(2, ByteBuf::writeShort),
        INT(3, ByteBuf::writeInt),
        LONG(4, ByteBuf::writeLong);

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

    public EntityBuilder writeByte(int value)
    {
        this.buffer.writeByte(value);
        return this;
    }

    public EntityBuilder writeInt(int value)
    {
        this.buffer.writeInt(value);
        return this;
    }

    public EntityBuilder writeLong(long value)
    {
        this.buffer.writeLong(value);
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

    public EntityBuilder writeBytes(Callable<ByteBuf> body)
    {
        try
        {
            return this.writeBytes(body.call());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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
