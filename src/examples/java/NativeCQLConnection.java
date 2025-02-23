import com.google.common.collect.ImmutableMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class NativeCQLConnection extends ChannelInboundHandlerAdapter implements Runnable
{
    private final Bootstrap client;
    private final NioEventLoopGroup group;

    private static final int PROTOCOL_VERSION = 0x04;
    
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Initializer initializer;
    private final Bootstrap handler;

    private static Function<Integer, Integer> streamId = (id) ->
    {
        if (id < 32768)
            return id;
        else
            throw new IllegalArgumentException("Invalid stream id: " + id);
    };

    public NativeCQLConnection()
    {
        this.client = new Bootstrap();
        this.group = new NioEventLoopGroup();
        this.initializer = new Initializer(this, "cassandra", "cassandra");

        this.handler = this.client.group(this.group)
                .channel(NioSocketChannel.class)
                .handler(this.initializer)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
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
        ByteBuf startup = this.initializer.createStartupMessage(ctx);

        ctx.writeAndFlush(startup.retain());
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

    private final class Initializer extends ChannelInitializer<SocketChannel>
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
        private static final String DRIVER_VERSION = "0.1.0";

        private static final String DRIVER_NAME_OPTION = "DRIVER_NAME";
        private static final String DRIVER_NAME = "mmorrii one love!";

        static final String COMPRESSION_OPTION = "COMPRESSION";
        static final String NO_COMPACT_OPTION = "NO_COMPACT";


        public ByteBuf createAuthResponse(ChannelHandlerContext ctx)
        {
            byte[] initialToken = initialResponse();
            ByteBuf buffer = Unpooled.buffer(initialToken.length);

            buffer.writeByte(PROTOCOL_VERSION); // Версия протокола (4)
            buffer.writeByte(0x00); // Флаги
            buffer.writeShort(0x00); // Stream ID
            buffer.writeByte(0x0F); // Opcode (AUTH_RESPONSE)

            buffer.writeInt(initialToken.length);
            buffer.writeBytes(initialToken);
            return buffer;
        }

        public ByteBuf createStartupMessage(ChannelHandlerContext context)
        {
            ByteBuf buffer = context.alloc().buffer();

            buffer.writeByte(PROTOCOL_VERSION); // Версия протокола (4)
            buffer.writeByte(0x00); // Флаги
            buffer.writeShort(0x00); // Stream ID
            buffer.writeByte(0x01); // Opcode (STARTUP)

            ImmutableMap.Builder<String, String> options = new ImmutableMap.Builder<>();
            options.put(CQL_VERSION_OPTION, CQL_VERSION);

            //options.put(COMPRESSION_OPTION, "");
            //options.put(NO_COMPACT_OPTION, "true");

            options.put(DRIVER_VERSION_OPTION, DRIVER_VERSION);
            options.put(DRIVER_NAME_OPTION, DRIVER_NAME);

            ByteBuf body = Unpooled.buffer();
            Writer.writeStringMap(options.build(), body);

            buffer.writeInt(body.readableBytes()); // Обновляем длину тела
            buffer.writeBytes(body); // Добавляем тело сообщения

            return buffer;
        }

        public ByteBuf registerMessage(ChannelHandlerContext context)
        {
            ByteBuf buffer = context.alloc().buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(0x0B);

            ArrayList<String> list = new ArrayList<>();

            list.add("SCHEMA_CHANGE");
            //list.add("TOPOLOGY_CHANGE");
            //list.add("STATUS_CHANGE");

            ByteBuf body = Unpooled.buffer();
            Writer.writeStringList(list, body);

            buffer.writeInt(body.readableBytes()); // Обновляем длину тела
            buffer.writeBytes(body); // Добавляем тело сообщения

            return buffer;
        }

        public ByteBuf createQuery(ChannelHandlerContext context, String query)
        {
            ByteBuf buffer = context.alloc().buffer();

            // Заголовок фрейма
            buffer.writeByte(PROTOCOL_VERSION); // Версия протокола (v4)
            buffer.writeByte(0x00); // Флаги
            buffer.writeShort(streamId.apply(0x00)); // Stream ID
            buffer.writeByte(0x07); // Код операции (QUERY)

            // Временное значение длины (заменим позже)
            int bodyLengthIndex = buffer.writerIndex();
            buffer.writeInt(0);

            // Тело фрейма
            int bodyStartIndex = buffer.writerIndex();
            Writer.writeLongString(query, buffer); // Записываем запрос
            buffer.writeShort(0x0001); // Consistency level (например, ONE)
            buffer.writeByte(0x00); // Флаги запроса

            // Обновляем длину тела фрейма
            int bodyLength = buffer.writerIndex() - bodyStartIndex;
            buffer.setInt(bodyLengthIndex, bodyLength);

            return buffer;
        }

        public ByteBuf createMessageOptions(ChannelHandlerContext context)
        {
            ByteBuf buffer = context.alloc().buffer();

            buffer.writeByte(PROTOCOL_VERSION); // Версия протокола (4)
            buffer.writeByte(0x00); // Флаги
            buffer.writeShort(0x00); // Stream ID
            buffer.writeByte(0x05); // Opcode (OPTIONS)

            buffer.writeInt(0);

            return buffer;
        }

        public byte[] initialResponse()
        {
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
        protected void encode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
        {
            out.add(byteBuf);
        }
    }

    private static final class MessageDecoder extends MessageToMessageDecoder<ByteBuf>
    {
        private final Initializer initializer;

        public boolean isFull = true;
        public int size = 0;
        public int finalSize = 0;

        public ByteBuf buffer = Unpooled.buffer();

        public MessageDecoder(Initializer initializer)
        {
            this.initializer = initializer;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception
        {
            if (!isFull) {
                int size = buffer.readableBytes();
                this.size = this.size + size;

                //System.out.println(ByteBufUtil.prettyHexDump(buffer));

                //buffer.resetReaderIndex();
                buffer = Unpooled.compositeBuffer().addComponents(true, buffer, byteBuf);
                //buffer.writeBytes(buffer, this.size);

                System.out.println(ByteBufUtil.prettyHexDump(buffer));
            } else {
                buffer = byteBuf.copy();
            }

            final int versionByte = (256 + buffer.readByte());

            int version = versionByte & 0x7F; // Версия протокола
            int flags = buffer.readByte();   // Флаги
            int streamId = buffer.readShort(); // Stream ID
            int opcode = buffer.readByte();  // Код операции

            boolean isResponse = (versionByte & 0x80) != 0;

            int length = buffer.readInt();   // Длина тела фрейма
            int lengthBuffer = buffer.readableBytes();

            if (length != lengthBuffer) {
                isFull = false;
                finalSize = length;
                size = lengthBuffer;
            }

            ByteBuf message = null;

            if (opcode == 0x03)
            {
                message = this.initializer.createAuthResponse(ctx);
            }

            if (opcode == 0x10)
            {
                message = this.initializer.createMessageOptions(ctx);
            }

            if (opcode == 0x06)
            {
                message = this.initializer.registerMessage(ctx);
            }

            if (opcode == 0x02)
            {
                message = this.initializer.createQuery(ctx, "SELECT * FROM system.clients");
            }

            if (message != null)
            {
                ctx.writeAndFlush(message);
            }

            System.out.println("version = " + version + " | isResponse = " + isResponse + " | flags = " + flags + " | streamId = " + streamId + " | opcode = " + opcode + " | length = " + length);
            //System.out.println(ByteBufUtil.prettyHexDump(buffer));

            if (finalSize <= size)
            {
                finalSize = 0;
                size = 0;
                buffer.clear();
            }
        }
    }

    public static final class Writer
    {
        public static void writeStringMap(Map<String, String> m, ByteBuf cb)
        {
            cb.writeShort(m.size());
            for (Map.Entry<String, String> entry : m.entrySet())
            {
                writeString(entry.getKey(), cb);
                writeString(entry.getValue(), cb);
            }
        }

        public static void writeStringList(List<String> list, ByteBuf cb)
        {
            cb.writeShort(list.size());
            list.forEach(element -> writeString(element, cb));
        }

        public static void writeString(String str, ByteBuf cb)
        {
            byte[] bytes = str.getBytes(CharsetUtil.UTF_8);
            cb.writeShort(bytes.length);
            cb.writeBytes(bytes);
        }

        public static void writeLongString(String str, ByteBuf cb)
        {
            byte[] bytes = str.getBytes(CharsetUtil.UTF_8);

            cb.writeInt(bytes.length);
            cb.writeBytes(bytes);
        }
    }
}
