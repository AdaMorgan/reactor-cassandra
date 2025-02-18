import com.datastax.api.sharding.DefaultObjectManagerBuilder;
import com.datastax.api.sharding.ObjectManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CassandraClient {
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public CassandraClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void start() throws Exception {

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // Добавляем декодеры/кодировщики
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(256 * 1024 * 1024, 4, 4, 0, 0, true),
                                    new LengthFieldPrepender(4)
                            );;
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
                        {
                            cause.printStackTrace();
                            ctx.close();
                        }
                    });

            ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port)).sync();
            Channel channel = future.channel();

            // Авторизуемся
            sendAuthRequest(channel);

            // Отправляем запрос
            //sendQuery(channel, "SELECT * FROM system.clients");

            // Ожидаем завершения
            future.channel().closeFuture().sync();
        } finally {
            //group.shutdownGracefully().sync();
        }
    }

    private void sendAuthRequest(Channel channel)
    {
        ByteBuf buffer = channel.alloc().buffer();
        buffer.writeInt(0x01);   // Version 1
        buffer.writeByte((byte) 0x81); // Opcode: Authenticate

        byte[] userBytes = username.getBytes(CharsetUtil.UTF_8);
        byte[] passBytes = password.getBytes(CharsetUtil.UTF_8);

        buffer.writeShort(userBytes.length + passBytes.length + 6);
        buffer.writeShort(userBytes.length);
        buffer.writeBytes(userBytes);
        buffer.writeShort(passBytes.length);
        buffer.writeBytes(passBytes);

        channel.writeAndFlush(buffer);
    }

    public static void main(String[] args) throws Exception
    {
        new CassandraClient("127.0.0.1", 9042, "cassandra", "cassandra").start();
    }
}