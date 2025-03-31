package com.datastax.internal.requests;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Requester
{
    public ByteBuf execute()
    {
        return Unpooled.buffer();
    }
}
