import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;


public class CassandraClient
{
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

    public void connect() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(@NotNull SocketChannel ch) {
                            ch.pipeline().addLast(new LengthFieldPrepender(2));
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1048576, 0, 2, 0, 2));
                            ch.pipeline().addLast(new ByteArrayEncoder());
                            ch.pipeline().addLast(new ByteArrayDecoder());
                            ch.pipeline().addLast(new CassandraClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static class CassandraClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // Отправка STARTUP-запроса для инициализации соединения
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeByte(0x01); // версия протокола
            buffer.writeByte(0x00); // флаги
            buffer.writeByte(0x00); // потоковый идентификатор
            buffer.writeByte(0x01); // опкод (STARTUP)
            buffer.writeInt(0x00000002); // длина тела

            // Тело запроса (CQL_VERSION и COMPRESSION)
            ByteBuf body = Unpooled.buffer();
            body.writeShort(0x0002); // количество параметров
            body.writeShort(0x0009); // длина ключа "CQL_VERSION"
            body.writeBytes("CQL_VERSION".getBytes(StandardCharsets.UTF_8));
            body.writeShort(0x0005); // длина значения "3.0.0"
            body.writeBytes("3.0.0".getBytes(StandardCharsets.UTF_8));
            body.writeShort(0x000A); // длина ключа "COMPRESSION"
            body.writeBytes("COMPRESSION".getBytes(StandardCharsets.UTF_8));
            body.writeShort(0x0004); // длина значения "none"
            body.writeBytes("none".getBytes(StandardCharsets.UTF_8));

            buffer.writeInt(body.readableBytes());
            buffer.writeBytes(body);

            ctx.writeAndFlush(buffer);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf in = (ByteBuf) msg;
            System.out.println("Received response: " + in.toString(StandardCharsets.UTF_8));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CassandraClient client = new CassandraClient("localhost", 9042, "username", "password");
        client.connect();
    }
}