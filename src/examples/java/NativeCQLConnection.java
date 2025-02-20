import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class NativeCQLConnection extends ChannelInboundHandlerAdapter implements Runnable
{
    private final Bootstrap client;
    private final NioEventLoopGroup group;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Initializer initializer;
    private final Bootstrap handler;

    public NativeCQLConnection()
    {
        this.client = new Bootstrap();
        this.group = new NioEventLoopGroup();
        this.initializer = new Initializer(this, "cassandra", "cassandra");

        this.handler = this.client.group(this.group).channel(NioSocketChannel.class).handler(this.initializer);
    }

    public static void main(String[] args)
    {
        new NativeCQLConnection().run();
    }

    @Override
    public void run()
    {
        handler.connect(HOST, PORT);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        ByteBuf startupMessage = this.initializer.createStartupMessage();
        ctx.writeAndFlush(startupMessage);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
    {
        this.group.shutdownGracefully();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
    }

    private static final class Initializer extends ChannelInitializer<SocketChannel>
    {
        private final ChannelHandler handler;
        private final byte[] username;
        private final byte[] password;

        public Initializer(ChannelHandler handler, String username, String password)
        {
            this.handler = handler;
            this.username = username.getBytes(StandardCharsets.UTF_8);
            this.password = password.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected void initChannel(SocketChannel channel) throws Exception
        {
            channel.pipeline().addLast(new MessageDecoder(this));
            channel.pipeline().addLast(new MessageEncoder(this));

            channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));

            channel.pipeline().addLast(this.handler);
        }

        private static final String CQL_VERSION_OPTION = "CQL_VERSION";
        private static final String CQL_VERSION = "3.0.0";
        private static final String DRIVER_VERSION_OPTION = "DRIVER_VERSION";
        private static final String DRIVER_NAME_OPTION = "DRIVER_NAME";
        private static final String DRIVER_NAME = "Apache Cassandra Java Driver";

        static final String COMPRESSION_OPTION = "COMPRESSION";
        static final String NO_COMPACT_OPTION = "NO_COMPACT";


        public ByteBuf createAuthResponse(ChannelHandlerContext ctx) {
            byte[] initialToken = initialResponse();
            ByteBuf buffer = ctx.alloc().buffer(initialToken.length);
            buffer.writeByte(0x0F); // AUTH_RESPONSE opcode
            buffer.writeInt(0); // Stream ID
            buffer.writeInt(initialToken.length);
            buffer.writeBytes(initialToken);
            return buffer;
        }

        public ByteBuf createStartupMessage() {
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeByte(0x04); // Версия протокола (4)
            buffer.writeByte(0x00); // Флаги
            buffer.writeByte(0x00); // Stream ID
            buffer.writeByte(0x01); // Opcode (STARTUP)
            buffer.writeInt(0); // Длина тела (пока неизвестна)

            ImmutableMap.Builder<String, String> options = new ImmutableMap.Builder<>();
            options.put(CQL_VERSION_OPTION, CQL_VERSION);
            options.put(COMPRESSION_OPTION, "");
            options.put(NO_COMPACT_OPTION, "true");
            options.put(DRIVER_VERSION_OPTION, "3.12.2-SNAPSHOT");
            options.put(DRIVER_NAME_OPTION, DRIVER_NAME);

            ByteBuf body = Unpooled.buffer();
            Writer.writeStringMap(options.build(), body);

            buffer.writeInt(body.readableBytes()); // Обновляем длину тела
            buffer.writeBytes(body); // Добавляем тело сообщения

            return buffer;
        }

        public byte[] initialResponse() {
            byte[] initialToken = new byte[username.length + password.length + 2];
            initialToken[0] = 0;
            System.arraycopy(username, 0, initialToken, 1, username.length);
            initialToken[username.length + 1] = 0;
            System.arraycopy(password, 0, initialToken, username.length + 2, password.length);
            return initialToken;
        }
    }

    private static final class MessageEncoder extends MessageToMessageEncoder<ByteBuf>
    {
        private final Initializer initializer;

        public MessageEncoder(Initializer initializer)
        {
            this.initializer = initializer;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            out.add(msg.copy());
        }
    }

    private static final class MessageDecoder extends MessageToMessageDecoder<ByteBuf> {

        private final Initializer initializer;

        public MessageDecoder(Initializer initializer)
        {
            this.initializer = initializer;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            System.out.println("Received raw data: " + msg.readableBytes() + " bytes");

            System.out.println(msg.toString(StandardCharsets.UTF_8));
            //System.out.println(ByteBufUtil.prettyHexDump(msg));

            if (msg.readableBytes() > 0) {
                byte opcodeByte = msg.readByte();
                int opcode = Byte.toUnsignedInt(opcodeByte); // Преобразуем в беззнаковый int
                System.out.println("Opcode: " + opcode);

                if (opcode == 0x03) { // AUTHENTICATE opcode
                    System.out.println("Server requires authentication");
                    // Отправьте AUTH_RESPONSE
                } else if (opcode == 0x0E) { // AUTH_CHALLENGE opcode
                    ByteBuf authResponse = this.initializer.createAuthResponse(ctx);
                    ctx.writeAndFlush(authResponse);
                } else {
                    System.out.println("Unknown opcode: " + opcode);
                }
            }
        }
    }

    public static final class Writer
    {
        public static void writeStringMap(Map<String, String> m, ByteBuf cb) {
            cb.writeShort(m.size());
            for (Map.Entry<String, String> entry : m.entrySet()) {
                writeString(entry.getKey(), cb);
                writeString(entry.getValue(), cb);
            }
        }

        public static void writeString(String str, ByteBuf cb) {
            byte[] bytes = str.getBytes(CharsetUtil.UTF_8);
            cb.writeShort(bytes.length);
            cb.writeBytes(bytes);
        }
    }
}
