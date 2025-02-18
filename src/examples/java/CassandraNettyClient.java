import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class CassandraNettyClient {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public CassandraNettyClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void connect() throws InterruptedException {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(@NotNull SocketChannel channel) throws Exception {
                    channel.pipeline().addLast(
                            new LengthFieldBasedFrameDecoder(256 * 1024 * 1024, 4, 4, 0, 0, true),
                            new LengthFieldPrepender(4),
                            new CassandraClientHandler(username, password));
                }
            });

            ChannelFuture f = b.connect(new InetSocketAddress(host, port)).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully().awaitUninterruptibly(10, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String host = "localhost";
        int port = 9042;
        String username = "cassandra";
        String password = "cassandra";

        CassandraNettyClient client = new CassandraNettyClient(host, port, username, password);
        client.connect();
    }

    private static class CassandraClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final String username;
        private final String password;

        public CassandraClientHandler(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // Send authentication request
            byte[] username = this.username.getBytes(CharsetUtil.UTF_8);
            byte[] password = this.password.getBytes(CharsetUtil.UTF_8);

            byte[] initialToken = new byte[username.length + password.length + 2];

            System.arraycopy(username, 0, initialToken, 1, username.length);
            initialToken[username.length + 1] = 0;
            System.arraycopy(password, 0, initialToken, username.length + 2, password.length);

            ctx.writeAndFlush(initialToken);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            // Handle incoming messages here
            System.out.println(msg.readByte());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}