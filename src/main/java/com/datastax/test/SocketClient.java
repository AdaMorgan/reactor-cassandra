package com.datastax.test;

import com.datastax.api.exceptions.ErrorResponse;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.utils.CustomLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SocketClient extends ChannelInboundHandlerAdapter
{
    public static final Logger LOG = CustomLogger.getLog(SocketClient.class);

    private static final int PROTOCOL_VERSION = 0x04;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Initializer initializer;
    private final Bootstrap handler;

    public SocketClient()
    {
        NioEventLoopGroup group = new NioEventLoopGroup();

        this.initializer = new Initializer(this, "cassandra", "cassandra");

        this.handler = new Bootstrap().group(group)
                .channel(NioSocketChannel.class)
                .handler(this.initializer)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    public static void main(String[] args)
    {
        new SocketClient().connect();
    }

    public synchronized void connect()
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
    public void channelInactive(@Nonnull ChannelHandlerContext ctx) throws Exception
    {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
    }

    static final class Initializer extends ChannelInitializer<SocketChannel>
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
            buffer.writeByte(SocketCode.AUTH_RESPONSE);

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
            list.add("TOPOLOGY_CHANGE");
            list.add("STATUS_CHANGE");

            ByteBuf body = Unpooled.buffer();
            Writer.writeStringList(list, body);

            buffer.writeInt(body.readableBytes());
            buffer.writeBytes(body);

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

        public ByteBuf ready()
        {
            //return this.prepare.prepare("SELECT * FROM system.clients WHERE shard_id = ? ALLOW FILTERING");

            //return this.createQuery(0, "SELECT * FROM system.clients");

            LOG.info("Finished Loading!");
            return null;
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
        protected void decode(ChannelHandlerContext ctx, ByteBuf frame, List<Object> out) throws Exception
        {
            this.queue.addLast(frame);
            processFullFrame(ctx, frame, out);
        }

        private ByteBuf processResultResponse(ByteBuf byteBuf)
        {
            int kind = byteBuf.readInt();

            switch (kind)
            {
                case 0x0001:
                    System.out.println("Void: for results carrying no information.");
                    break;
                case 0x0002:
                    System.out.println("Rows: for results to select queries, returning a set of rows.");
                    break;
                case 0x0003:
                    System.out.println("Set_keyspace: the result to a `use` query.");
                    break;
                case 0x0004:
                    System.out.println("Prepared: result to a PREPARE message.");
                    break;
                case 0x0005:
                    System.out.println("Schema_change: the result to a schema altering query.");
                    break;
                default:
                    System.out.println("Received unknown result type: " + kind);
                    break;
            }

            return null;
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
                    return this.initializer.ready();
                case SocketCode.RESULT:
                    return this.processResultResponse(buffer);
                default:
                    throw new UnsupportedOperationException("Unsupported opcode: " + opcode);
            }
        }

        public void processFullFrame(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out)
        {
            byte versionHeaderByte = byteBuf.readByte();
            int version = (256 + versionHeaderByte) & 0x7F;
            boolean isResponse = ((256 + versionHeaderByte) & 0x80) != 0;

            byte flags = byteBuf.readByte();
            short streamId = byteBuf.readShort();
            byte opcode = byteBuf.readByte();
            int length = byteBuf.readInt();

            ByteBuf message = handle(opcode, byteBuf, streamId);

            if (message != null)
            {
                ctx.writeAndFlush(message);
            }

            byteBuf.resetReaderIndex();
            out.add(byteBuf.retain());
        }

        private ByteBuf error(ByteBuf buffer, int streamId)
        {
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
