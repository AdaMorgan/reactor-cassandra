package com.datastax.api;

import io.netty.buffer.ByteBuf;

public class EntityBuilder
{
    private final ByteBuf byteBuf;

    public EntityBuilder(ByteBuf byteBuf)
    {
        this.byteBuf = byteBuf;
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
