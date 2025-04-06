package com.datastax.test;

import com.datastax.api.Library;
import com.datastax.api.requests.ObjectAction;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.utils.CustomLogger;
import com.datastax.test.action.PrepareCreateActionImpl;
import com.datastax.test.action.QueryCreateActionImpl;
import com.datastax.test.action.session.LoginCreateActionImpl;
import com.datastax.test.action.session.OptionActionImpl;
import com.datastax.test.action.session.RegisterActionImpl;
import com.datastax.test.action.session.StartingActionImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SocketClient extends ChannelInboundHandlerAdapter
{
    public static final Logger LOG = CustomLogger.getLog(SocketClient.class);

    private static final byte PROTOCOL_VERSION = 0x04;
    private static final byte DEFAULT_FLAG = 0x00;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 9042;
    private final Bootstrap handler;
    private final LibraryImpl library;
    private final AtomicReference<ChannelHandlerContext> context = new AtomicReference<>();

    public SocketClient(LibraryImpl library)
    {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup group = new NioEventLoopGroup();
        Initializer initializer = new Initializer(this);

        this.library = library;
        this.handler = bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(initializer)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true);
    }

    public synchronized void connect()
    {
        handler.connect(HOST, PORT);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext context)
    {
        this.library.setStatus(Library.Status.CONNECTING_TO_SOCKET);
    }


    public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";
    public static final String TEST_QUERY = "SELECT * FROM system.clients";

    int count = 0;

    @Override
    public void channelActive(@Nonnull ChannelHandlerContext context)
    {
        this.library.setStatus(Library.Status.IDENTIFYING_SESSION);
        this.context.set(context);

        new StartingActionImpl(this.library, PROTOCOL_VERSION, DEFAULT_FLAG).queue(node ->
        {
            System.out.println("starting!");
            new LoginCreateActionImpl(this.library, PROTOCOL_VERSION, DEFAULT_FLAG).queue(authSuccess ->
            {
                System.out.println("authSuccess!");
                new OptionActionImpl(this.library, PROTOCOL_VERSION, DEFAULT_FLAG).queue(supported ->
                {
                    System.out.println("supported!");
                    new RegisterActionImpl(this.library, PROTOCOL_VERSION, DEFAULT_FLAG).queue(ready ->
                    {
                        System.out.println("ready!");

                        for (int i = 0; i < 10; i++)
                        {
                            new QueryCreateActionImpl(this.library, PROTOCOL_VERSION, DEFAULT_FLAG, TEST_QUERY, ObjectAction.Level.ONE).queue(result ->
                            {
                                count++;

                                ObjectActionImpl.LOG.info("count: {}", count);
                                new RowsResultImpl(result).run();
                            });

                            new PrepareCreateActionImpl(this.library, PROTOCOL_VERSION, DEFAULT_FLAG, TEST_QUERY_PREPARED, ObjectAction.Level.ONE).queue(prepare ->
                            {
                                new RowsResultImpl(prepare).run();
                            });
                        }
                    });
                });
            });
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable failure)
    {
        failure.printStackTrace();
    }

    private final Map<Short, Consumer<? super Response>> queue = new ConcurrentHashMap<>();
    private final Queue<Request<?>> cacheRequest = new ConcurrentLinkedQueue<>();
    private final Queue<ByteBuf> cacheResponse = new ConcurrentLinkedQueue<>();

    public <R> void execute(Request<R> request, short stream)
    {
        ChannelHandlerContext context = this.context.get();

        if (context != null && !this.queue.containsKey(stream))
        {
            ByteBuf body = request.getBody();

            request.getBody().setShort(2, stream);

            request.handleResponse(stream, this.queue::put);

            context.writeAndFlush(body.retain());
        }
        else
        {
            cacheRequest.add(request);
        }
    }

    public static final class Initializer extends ChannelInitializer<SocketChannel>
    {
        private final SocketClient client;

        public Initializer(SocketClient client)
        {
            this.client = client;
        }

        @Override
        protected void initChannel(@Nonnull SocketChannel channel) throws Exception
        {
            if (SocketConfig.IS_DEBUG)
            {
                channel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            }

            channel.pipeline().addLast(new MessageDecoder(this));
            channel.pipeline().addLast(new MessageEncoder());

            channel.pipeline().addLast(this.client);
        }
    }

    private static final class MessageEncoder extends MessageToMessageEncoder<ByteBuf>
    {
        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out)
        {
            out.add(msg.retain());
        }
    }

    private static final class MessageDecoder extends MessageToMessageDecoder<ByteBuf>
    {
        private final Initializer initializer;
        private final LibraryImpl api;

        public MessageDecoder(Initializer initializer)
        {
            this.api = initializer.client.library;
            this.initializer = initializer;
        }

        @Override
        protected void decode(ChannelHandlerContext context, ByteBuf frame, List<Object> out)
        {
            enqueue(context, frame, out);
        }

        public void setFlagPage(ByteBuf buffer)
        {
            byte currentByte = buffer.getByte(1);
            int newByte = currentByte | 0x16;
            buffer.setByte(1, newByte);
        }

        public ByteBuf getCompositeBuffer(ByteBuf buffer)
        {
            for (ByteBuf frame : this.initializer.client.cacheResponse)
            {
                if ((frame.getByte(1) & 0x16) == 0x016)
                {
                    return Unpooled.wrappedBuffer(frame, buffer);
                }
            }

            return buffer;
        }

        public void enqueue(ChannelHandlerContext context, ByteBuf frame, List<Object> out)
        {
            ByteBuf compositeFrame = getCompositeBuffer(frame);

            byte versionHeader = compositeFrame.readByte();

            byte version = (byte) ((256 + versionHeader) & 0x7F);
            boolean isResponse = ((256 + versionHeader) & 0x80) != 0;

            byte flags = compositeFrame.readByte();
            short stream = compositeFrame.readShort();
            byte opcode = compositeFrame.readByte();

            int length = compositeFrame.readInt();

            directBuffer(context, version, isResponse, flags, stream, opcode, length, compositeFrame);
        }

        private void directBuffer(ChannelHandlerContext context, byte version, boolean isResponse, byte flags, short stream, byte opcode, int length, ByteBuf frame)
        {
            Queue<Request<?>> cacheRequest = this.initializer.client.cacheRequest;

            if (frame.readableBytes() == length)
            {
                flags = (byte) (flags & ~0x16);

                frame.setByte(1, flags);

                Consumer<? super Response> consumer = this.initializer.client.queue.remove(stream);
                consumer.accept(new Response(version, flags, stream, opcode, length, frame));

                if (!cacheRequest.isEmpty())
                {
                    Request<?> peek = cacheRequest.peek();
                    this.api.getRequester().execute(peek);
                    cacheRequest.remove(peek);
                }
            }
            else
            {
                setFlagPage(frame);
                frame.resetReaderIndex();

                this.initializer.client.cacheResponse.offer(frame.copy());
            }
        }
    }
}
