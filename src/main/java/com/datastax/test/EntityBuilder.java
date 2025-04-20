package com.datastax.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.example.data.DataObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    public enum EntityType
    {
        BYTE(), ;

        EntityType()
        {
            DataObject
        }

        public static void main(String[] args)
        {

            System.out.println();
        }
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

    public EntityBuilder writeString(String in)
    {
        return this.writeString(in, ByteBuf::writeInt);
    }

    public EntityBuilder writeString(String in, BiConsumer<ByteBuf, Integer> length)
    {
        byte[] bytes = in.getBytes(StandardCharsets.UTF_8);
        length.accept(buffer, bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public EntityBuilder writeString(String... arr)
    {
        EntityBuilder collect = Arrays.stream(arr).collect(EntityBuilder::new, EntityBuilder::writeString, EntityBuilder::writeBytes);
        return this.writeBytes(collect);
    }

    public EntityBuilder writeString(BiConsumer<ByteBuf, Integer> length, String... arr)
    {
        EntityBuilder collect = Arrays.stream(arr).collect(EntityBuilder::new, (builder, in) -> builder.writeString(in, length), EntityBuilder::writeBytes);
        return this.writeBytes(collect);
    }

    public EntityBuilder writeBytes(EntityBuilder in)
    {
        this.writeBytes(in.buffer);
        return this;
    }

    public EntityBuilder writeBytes(ByteBuf in)
    {
        int len = in.readableBytes();
        this.buffer.writeInt(len);
        this.buffer.writeBytes(in, in.readerIndex(), len);
        return this;
    }

    public EntityBuilder writeBytes(byte[] bytes)
    {
        this.buffer.writeInt(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public EntityBuilder apply(Consumer<? super ByteBuf> callback)
    {
        callback.accept(this.buffer);
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
