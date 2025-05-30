package com.github.adamorgan.internal.utils.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class MessageEncoder extends MessageToByteEncoder<ByteBuf>
{

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception
    {
        out.writeBytes(msg);
    }
}
