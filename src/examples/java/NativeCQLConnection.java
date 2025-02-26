import com.datastax.internal.request.ErrorResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
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

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class NativeCQLConnection extends ChannelInboundHandlerAdapter implements Runnable
{
    private final Bootstrap client;
    private final NioEventLoopGroup group;

    private static final int PROTOCOL_VERSION = 0x04;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Initializer initializer;
    private final Bootstrap handler;

    private static final Function<Integer, Integer> streamId = (id) ->
    {
        if (id < 32768)
        {
            return id;
        }
        else
        {
            throw new IllegalArgumentException("Invalid stream id: " + id);
        }
    };

    public NativeCQLConnection()
    {
        this.client = new Bootstrap();
        this.group = new NioEventLoopGroup();
        this.initializer = new Initializer(this, "cassandra", "cassandra");

        this.handler = this.client.group(this.group).channel(NioSocketChannel.class).handler(this.initializer).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
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
        private static final String DRIVER_VERSION = "0.1.0";

        private static final String DRIVER_NAME_OPTION = "DRIVER_NAME";
        private static final String DRIVER_NAME = "mmorrii one love!";

        static final String COMPRESSION_OPTION = "COMPRESSION";
        static final String NO_COMPACT_OPTION = "NO_COMPACT";


        public ByteBuf createAuthResponse(ChannelHandlerContext ctx)
        {
            byte[] initialToken = initialResponse();
            ByteBuf buffer = Unpooled.buffer(initialToken.length);

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(0x0F);

            buffer.writeInt(initialToken.length);
            buffer.writeBytes(initialToken);
            return buffer;
        }

        public ByteBuf createStartupMessage(ChannelHandlerContext context)
        {
            ByteBuf buffer = context.alloc().buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(0x01);

            Map<String, String> options = new HashMap<>();
            options.put(CQL_VERSION_OPTION, CQL_VERSION);

            //options.put(COMPRESSION_OPTION, "");
            //options.put(NO_COMPACT_OPTION, "true");

            options.put(DRIVER_VERSION_OPTION, DRIVER_VERSION);
            options.put(DRIVER_NAME_OPTION, DRIVER_NAME);

            ByteBuf body = Unpooled.buffer();
            Writer.writeStringMap(options, body);

            buffer.writeInt(body.readableBytes());
            buffer.writeBytes(body);

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

            buffer.writeInt(body.readableBytes());
            buffer.writeBytes(body);

            return buffer;
        }

        public ByteBuf createQuery(ChannelHandlerContext context, String query)
        {
            ByteBuf buffer = context.alloc().buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(streamId.apply(0x00));
            buffer.writeByte(0x07);
            int bodyLengthIndex = buffer.writerIndex();
            buffer.writeInt(0);

            int bodyStartIndex = buffer.writerIndex();
            Writer.writeLongString(query, buffer);
            buffer.writeShort(0x0001);
            buffer.writeByte(0x00);

            int bodyLength = buffer.writerIndex() - bodyStartIndex;
            buffer.setInt(bodyLengthIndex, bodyLength);

            return buffer;
        }

        @Nonnull
        public ByteBuf createMessageOptions(ChannelHandlerContext context)
        {
            ByteBuf buffer = context.alloc().buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(0x05);

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
        private final LinkedList<ByteBuf> queue = new LinkedList<>();

        public MessageDecoder(Initializer initializer)
        {
            this.initializer = initializer;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception
        {
            if (queue.isEmpty() || isLast())
            {
                byte versionHeaderByte = buffer.readByte();

                byte flags = buffer.readByte();
                short streamId = buffer.readShort();
                byte opcode = buffer.readByte();
                int length = buffer.readInt();

                ByteBuf accumulatedBuffer = ctx.alloc().buffer(length);

                accumulatedBuffer.writeByte(versionHeaderByte);
                accumulatedBuffer.writeByte(flags);
                accumulatedBuffer.writeShort(streamId);
                accumulatedBuffer.writeByte(opcode);
                accumulatedBuffer.writeInt(length);
                accumulatedBuffer.writeBytes(buffer);

                this.queue.add(accumulatedBuffer);
            }
            else
            {
                this.queue.getLast().writeBytes(buffer);
            }

            if (isLast())
            {
                ByteBuf fullFrame = this.queue.getLast();
                processFullFrame(ctx, fullFrame, out);
            }
        }

        public boolean isLast()
        {
            return ! this.queue.isEmpty() && (this.queue.getLast().readableBytes() >= this.queue.getLast().getInt(5));
        }

        private void processResultResponse(ByteBuf buffer)
        {
            int kind = buffer.readInt();

            switch (kind)
            {
                case 2: // SELECT
                    processRowsResult(buffer);
                    break;
                case 1: // VOID
                    System.out.println("Received VOID result");
                    break;
                case 3: // SET_KEYSPACE
                    System.out.println("Received SET_KEYSPACE result");
                    break;
                default:
                    System.out.println("Received unknown result type: " + kind);
                    break;
            }
        }

        private void processRowsResult(ByteBuf buffer)
        {
            int flags = buffer.readInt();
            int columnsCount = buffer.readInt();

            boolean globalTablesSpec = (flags & 0x01) != 0;

            int length = buffer.readUnsignedShort();

            Supplier<String> message = () -> {
                byte[] bytes = new byte[length];
                buffer.readBytes(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            };

            List<ColumnMetadata> columns = new ArrayList<>();
            for (int i = 0; i < columnsCount; i++)
            {
                String keyspace = globalTablesSpec ? null : message.get();
                String table = globalTablesSpec ? null : message.get();
                String name = message.get();
                int type = buffer.readUnsignedShort();
                columns.add(new ColumnMetadata(keyspace, table, name, type));
            }

            int rowsCount = buffer.readInt();
            for (int i = 0; i < rowsCount; i++)
            {
                for (ColumnMetadata column : columns)
                {
                    if (length >= 0)
                    {
                        if (column.type == Column.Type.INET)
                        {

                        }
                    }
                }
            }
        }

        public static class ColumnMetadata
        {
            public final String keyspace;
            public final String table;
            public final String name;
            public final Column.Type type;

            ColumnMetadata(String keyspace, String table, String name, int type)
            {
                this.keyspace = keyspace;
                this.table = table;
                this.name = name;
                this.type = Column.Type.fromId(type);
            }
        }

        private String readString(ByteBuf buffer)
        {
            int length = buffer.readUnsignedShort();
            byte[] bytes = new byte[length];
            buffer.readBytes(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private void processFullFrame(ChannelHandlerContext ctx, ByteBuf fullFrame, List<Object> out)
        {
            byte versionHeaderByte = fullFrame.readByte();
            int version = (256 + versionHeaderByte) & 0x7F;
            boolean isResponse = ((256 + versionHeaderByte) & 0x80) != 0;

            byte flags = fullFrame.readByte();
            short streamId = fullFrame.readShort();
            byte opcode = fullFrame.readByte();
            int length = fullFrame.readInt();

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

            if (opcode == 0x08)
            {
                processResultResponse(fullFrame);
            }

            if (message != null)
            {
                ctx.writeAndFlush(message);
            }

            if (opcode == 0x00)
            {
                error(version, isResponse, flags, streamId, opcode, length, fullFrame);
            }
        }

        private void error(int version, boolean isResponse, byte flags, short streamId, byte opcode, int length, ByteBuf buffer)
        {
            int codeError = buffer.readInt();
            String message = readString(buffer);
            System.err.println(ErrorResponse.fromCode(codeError).name() + ": " + message);
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
