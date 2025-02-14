import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.nio.charset.StandardCharsets;

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
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // Increase max frame size to 16 MiB (or higher if needed)
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(16777216, 5, 4, 0, 4));
                            ch.pipeline().addLast(new LengthFieldPrepender(4));
                            ch.pipeline().addLast(new CassandraClientHandler(username, password));
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CassandraNettyClient client = new CassandraNettyClient("localhost", 9042, "cassandra", "cassandra");
        client.connect();
    }
}

class CassandraClientHandler extends ChannelInboundHandlerAdapter {

    private final String username;
    private final String password;
    private boolean authenticated = false;

    public CassandraClientHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Connected to Cassandra. Starting authentication...");
        sendAuthResponse(ctx);
        sendStartupMessage(ctx);
    }

    private void sendStartupMessage(ChannelHandlerContext ctx) {
        ByteBuf buffer = Unpooled.buffer();
        // Startup message for CQL protocol
        buffer.writeByte(0x03); // Protocol version (CQL binary protocol v4)
        buffer.writeByte(0x03); // Flags
        buffer.writeByte(0x03); // Stream ID
        buffer.writeByte(0x01); // Opcode: STARTUP
        buffer.writeInt(0x00000002); // Body length (2 for "CQL_VERSION")

        // CQL_VERSION
        buffer.writeShort(0x0006); // Length of "CQL_VERSION"
        buffer.writeBytes("CQL_VERSION".getBytes(StandardCharsets.UTF_8));
        buffer.writeShort(0x0005); // Length of "3.0.0"
        buffer.writeBytes("3.0.0".getBytes(StandardCharsets.UTF_8));

        ctx.writeAndFlush(buffer);
    }

    private void sendAuthResponse(ChannelHandlerContext ctx) {
        ByteBuf buffer = Unpooled.buffer();
        // SASL authentication response
        String credentials = "\0" + username + "\0" + password;
        byte[] credentialsBytes = credentials.getBytes(StandardCharsets.UTF_8);

        buffer.writeByte(0x04); // Protocol version
        buffer.writeByte(0x00); // Flags
        buffer.writeByte(0x00); // Stream ID
        buffer.writeByte(0x0F); // Opcode: AUTH_RESPONSE
        buffer.writeInt(credentialsBytes.length); // Body length
        buffer.writeBytes(credentialsBytes);

        ctx.writeAndFlush(buffer);
    }

    private void sendQuery(ChannelHandlerContext ctx) {
        ByteBuf buffer = Unpooled.buffer();
        String query = "SELECT * FROM system.clients;";
        byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);

        buffer.writeByte(0x04); // Protocol version
        buffer.writeByte(0x00); // Flags
        buffer.writeByte(0x00); // Stream ID
        buffer.writeByte(0x07); // Opcode: QUERY
        buffer.writeInt(queryBytes.length); // Body length
        buffer.writeBytes(queryBytes);

        ctx.writeAndFlush(buffer);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("Received response from Cassandra:");
        System.out.println(in.toString(StandardCharsets.UTF_8));

        if (!authenticated) {
            System.out.println("Sending authentication response...");
            sendAuthResponse(ctx);
            authenticated = true;
        } else {
            System.out.println("Sending query...");
            sendQuery(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}