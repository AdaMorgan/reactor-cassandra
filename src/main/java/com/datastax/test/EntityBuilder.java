package com.datastax.test;

import com.datastax.api.utils.data.DataType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufConvertible;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EntityBuilder implements ByteBufConvertible
{
    protected final ByteBuf buffer;
    protected final Consumer<? super ByteBuf> callback;

    public EntityBuilder()
    {
        this(null);
    }

    public EntityBuilder(@Nullable Consumer<? super ByteBuf> callback)
    {
        this.buffer = Unpooled.directBuffer();
        this.callback = callback;
    }

    public EntityBuilder put(byte value)
    {
        this.buffer.writeByte(value);
        return this;
    }

    public EntityBuilder put(int value)
    {
        this.buffer.writeInt(value);
        return this;
    }

    public EntityBuilder put(long value)
    {
        this.buffer.writeLong(value);
        return this;
    }

    public EntityBuilder put(short value)
    {
        this.buffer.writeShort(value);
        return this;
    }

    public EntityBuilder put(String in)
    {
        return this.put(in, ByteBuf::writeInt);
    }

    public EntityBuilder put(String in, BiConsumer<ByteBuf, Integer> length)
    {
        DataType.LONG_STRING.encode(buffer, in);
        return this;
    }

    public EntityBuilder put(String... arr)
    {
        EntityBuilder collect = Arrays.stream(arr).collect(EntityBuilder::new, EntityBuilder::put, EntityBuilder::put);
        return this.put(collect);
    }

    public EntityBuilder put(BiConsumer<ByteBuf, Integer> length, String... arr)
    {
        EntityBuilder collect = Arrays.stream(arr).collect(EntityBuilder::new, (builder, in) -> builder.put(in, length), EntityBuilder::put);
        return this.put(collect);
    }

    public EntityBuilder put(EntityBuilder in)
    {
        this.put(in.buffer);
        return this;
    }

    public EntityBuilder put(ByteBuf in)
    {
        int len = in.readableBytes();
        this.buffer.writeInt(len);
        this.buffer.writeBytes(in, in.readerIndex(), len);
        return this;
    }

    public EntityBuilder put(byte[] bytes)
    {
        this.buffer.writeInt(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    @Override
    public ByteBuf asByteBuf()
    {
        if (callback != null)
        {
            this.callback.accept(this.buffer);
        }
        return this.buffer;
    }

    @Override
    public String toString()
    {
        return ByteBufUtil.prettyHexDump(buffer);
    }
}
