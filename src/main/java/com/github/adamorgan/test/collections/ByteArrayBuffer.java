package com.github.adamorgan.test.collections;

import io.netty.buffer.ByteBuf;

import java.util.Collection;
import java.util.LinkedList;

public final class ByteArrayBuffer extends LinkedList<ByteBuf> implements Collection<ByteBuf>
{
    @Override
    public void addLast(ByteBuf byteBuf)
    {
        super.addLast(byteBuf);
    }

    @Override
    public void addFirst(ByteBuf byteBuf)
    {
        super.addFirst(byteBuf);
    }
}
