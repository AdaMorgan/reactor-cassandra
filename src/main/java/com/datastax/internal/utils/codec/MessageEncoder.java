package com.datastax.internal.utils.codec;

import com.datastax.internal.requests.SocketClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class MessageEncoder extends MessageToByteEncoder<ByteBuf>
{
    private final SocketClient.Initializer initializer;

    public MessageEncoder(SocketClient.Initializer initializer)
    {
        this.initializer = initializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception
    {
        out.writeBytes(msg);
    }
}
