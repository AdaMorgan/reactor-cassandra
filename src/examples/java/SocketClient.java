import com.datastax.internal.requests.ErrorResponse;
import com.datastax.internal.requests.SocketCode;
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
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class SocketClient extends ChannelInboundHandlerAdapter implements Runnable
{
    private final Bootstrap client;
    private final NioEventLoopGroup group;

    private static final int PROTOCOL_VERSION = 0x04;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Initializer initializer;
    private final Bootstrap handler;

    private static final Function<Integer, Integer> isStreamId = (id) ->
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

    public SocketClient()
    {
        this.client = new Bootstrap();
        this.group = new NioEventLoopGroup();
        this.initializer = new Initializer(this, "cassandra", "cassandra");
        this.handler = this.client.group(this.group).channel(NioSocketChannel.class).handler(this.initializer).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    public static void main(String[] args)
    {
        new SocketClient().run();
    }

    @Override
    public void run()
    {
        handler.connect(HOST, PORT);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        ByteBuf startup = this.initializer.createStartupMessage();

        ctx.writeAndFlush(startup.retain());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception
    {

    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception
    {

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
            //channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));

            channel.pipeline().addLast(new MessageDecoder(this));
            channel.pipeline().addLast(new MessageEncoder(this));

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


        public ByteBuf login()
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

        public ByteBuf createStartupMessage()
        {
            ByteBuf buffer = Unpooled.buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(SocketCode.STARTUP);

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

        public ByteBuf registerMessage()
        {
            ByteBuf buffer = Unpooled.buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(SocketCode.REGISTER);

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

        public ByteBuf createQuery(int streamId, String query)
        {
            ByteBuf buffer = Unpooled.buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(isStreamId.apply(streamId));
            buffer.writeByte(SocketCode.QUERY);
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

        public ByteBuf createMessageOptions()
        {
            ByteBuf buffer = Unpooled.buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(SocketCode.OPTIONS);

            buffer.writeInt(0);

            return buffer;
        }

        public byte[] initialResponse()
        {
            ByteBuf initialToken = Unpooled.buffer(username.length + password.length + 2);

            initialToken.writeByte(0);
            initialToken.writeBytes(username);
            initialToken.writeByte(0);
            initialToken.writeBytes(password);

            return initialToken.array();
        }

        public ByteBuf onReady()
        {
            return new CassandraExecuteExample().prepare(4, 0, "SELECT * FROM system.clients WHERE shard_id = ? ALLOW FILTERING");
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

                ByteBuf accumulatedBuffer = Unpooled.buffer(length);

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
            return isLast(false);
        }

        public boolean isLast(boolean state)
        {
            if (state)
            {
                System.out.println(this.queue.getLast().readableBytes() + ">=" + this.queue.getLast().getInt(5));
            }
            return !this.queue.isEmpty() && (this.queue.getLast().readableBytes() >= this.queue.getLast().getInt(5));
        }

        public boolean isLast(ByteBuf message)
        {
            return (message.readableBytes() >= message.getInt(5));
        }

        private ByteBuf processResultResponse(ByteBuf byteBuf, int streamId)
        {
            int kind = byteBuf.readInt();

            switch (kind)
            {
                case 0x0001:
                    System.out.println("Void: for results carrying no information.");
                    break;
                case 0x0002:
                    System.out.println("Rows: for results to select queries, returning a set of rows.");
                    return processRowsResult(byteBuf);
                case 0x0003:
                    System.out.println("Set_keyspace: the result to a `use` query.");
                    break;
                case 0x0004:
                    System.out.println("Prepared: result to a PREPARE message.");
                    System.out.println(ByteBufUtil.prettyHexDump(byteBuf));
                    return new CassandraExecuteExample().execute(byteBuf, 0);
                case 0x0005:
                    System.out.println("Schema_change: the result to a schema altering query.");
                    break;
                default:
                    System.out.println("Received unknown result type: " + kind);
                    break;
            }

            return null;
        }

        private ByteBuf processRowsResult(ByteBuf buffer)
        {
            int flags = buffer.readInt();
            int columnsCount = buffer.readInt();

            boolean globalTablesSpec = (flags & 0x01) != 0;

            String message = buffer.readCharSequence(buffer.readUnsignedShort(), StandardCharsets.UTF_8).toString();

            List<ColumnMetadata> columns = new ArrayList<>();

            for (int i = 0; i < columnsCount; i++)
            {
                String keyspace = globalTablesSpec ? null : message;
                String table = globalTablesSpec ? null : message;
                int type = buffer.readUnsignedShort();
                columns.add(new ColumnMetadata(keyspace, table, message, type));
            }

            //            int rowsCount = buffer.readInt();
            //
            //            for (int i = 0; i < rowsCount; i++)
            //            {
            //                for (ColumnMetadata column : columns)
            //                {
            //                    if (length >= 0)
            //                    {
            //                        if (column.type == Column.Type.INET)
            //                        {
            //
            //                        }
            //                    }
            //                }
            //            }

            return this.queue.getLast();
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

        public ByteBuf handle(int opcode, ByteBuf buffer, int streamId)
        {
            switch (opcode)
            {
                case SocketCode.ERROR:
                    return this.error(buffer, streamId);
                case SocketCode.AUTHENTICATE:
                    return this.initializer.login();
                case SocketCode.AUTH_SUCCESS:
                    return this.initializer.createMessageOptions();
                case SocketCode.SUPPORTED:
                    return this.initializer.registerMessage();
                case SocketCode.READY:
                    return this.initializer.onReady();
                case SocketCode.RESULT:
                    return this.processResultResponse(buffer, streamId);
                default:
                    throw new UnsupportedOperationException("Unsupported opcode: " + opcode);
            }
        }

        private void processFullFrame(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out)
        {
            byte versionHeaderByte = byteBuf.readByte();
            int version = (256 + versionHeaderByte) & 0x7F;
            boolean isResponse = ((256 + versionHeaderByte) & 0x80) != 0;

            byte flags = byteBuf.readByte();
            short streamId = byteBuf.readShort();
            byte opcode = byteBuf.readByte();
            int length = byteBuf.readInt();

            ByteBuf message = handle(opcode, byteBuf, streamId);

            ctx.writeAndFlush(message.retain());

            byteBuf.resetReaderIndex();
            out.add(byteBuf.retain());
        }

        private ByteBuf error(ByteBuf buffer, int streamId)
        {
            System.out.println(ByteBufUtil.prettyHexDump(buffer));
            int codeError = buffer.readInt();
            String message = buffer.readCharSequence(buffer.readUnsignedShort(), StandardCharsets.UTF_8).toString();

            throw new RuntimeException(streamId + " || " + ErrorResponse.fromCode(codeError).name() + ": " + message);
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
