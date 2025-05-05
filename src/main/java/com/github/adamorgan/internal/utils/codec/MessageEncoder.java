package com.github.adamorgan.internal.utils.codec;

import com.github.adamorgan.internal.requests.SocketClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public final class MessageEncoder extends MessageToByteEncoder<ByteBuf>
{
    private final SocketClient client;

    public MessageEncoder(SocketClient.ReliableFrameHandler handler)
    {
        this.client = handler.getClient();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception
    {
        out.writeBytes(msg);
    }
}
