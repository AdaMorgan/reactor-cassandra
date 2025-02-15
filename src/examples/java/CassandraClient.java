import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;

import java.nio.charset.StandardCharsets;

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

    public void connect() throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new CustomLengthFieldBasedFrameDecoder(1));
                            ch.pipeline().addLast(new LengthFieldPrepender(4));
                            ch.pipeline().addLast(new CassandraClientHandler(username, password));
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully(); // Shutdown bootstrap
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CassandraClient client = new CassandraClient("localhost", 9042, "cassandra", "cassandra");
        client.connect();
    }
}

class CassandraClientHandler extends ChannelInboundHandlerAdapter {

    private final String username;
    private final String password;

    public CassandraClientHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Отправка STARTUP-запроса для инициализации соединения
        ByteBuf startupMessage = createStartupMessage();
        ctx.writeAndFlush(startupMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;

        // После получения ответа на STARTUP, отправляем запрос на авторизацию
        System.out.println("Received response: " + in.toString(StandardCharsets.UTF_8));

        ByteBuf authMessage = createAuthMessage(username, password);
        ctx.writeAndFlush(authMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private ByteBuf createStartupMessage() {
        // Формируем STARTUP-запрос
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0x04); // Версия протокола (v4)
        buffer.writeByte(0x00); // Флаги
        buffer.writeByte(0x00); // Stream ID
        buffer.writeByte(0x01); // Код операции (STARTUP)

        // Тело запроса (пустой MAP для STARTUP)
        buffer.writeShort(1); // Количество элементов в MAP
        buffer.writeShort("CQL_VERSION".length());
        buffer.writeBytes("CQL_VERSION".getBytes(StandardCharsets.UTF_8));
        buffer.writeShort("3.0.0".length());
        buffer.writeBytes("3.0.0".getBytes(StandardCharsets.UTF_8));

        return buffer;
    }

    private ByteBuf createAuthMessage(String username, String password) {
        // Формируем AUTHENTICATE-запрос
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0x04); // Версия протокола (v4)
        buffer.writeByte(0x00); // Флаги
        buffer.writeByte(0x00); // Stream ID
        buffer.writeByte(0x02); // Код операции (AUTHENTICATE)

        // Тело запроса (логин и пароль)
        buffer.writeShort(username.length());
        buffer.writeBytes(username.getBytes(StandardCharsets.UTF_8));
        buffer.writeShort(password.length());
        buffer.writeBytes(password.getBytes(StandardCharsets.UTF_8));

        return buffer;
    }
}