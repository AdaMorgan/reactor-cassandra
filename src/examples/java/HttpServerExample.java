/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.adamorgan.api.LibraryBuilder;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class HttpServerExample extends ListenerAdapter
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM system.clients WHERE connection_stage = :stage ALLOW FILTERING";

    public static void main(String[] args) throws Exception
    {
        InetSocketAddress address = InetSocketAddress.createUnresolved("127.0.0.1", 9042);

        LibraryImpl api = LibraryBuilder.createLight(address, "cassandra", "cassandra")
                .addEventListeners(new HttpServerExample())
                .setCompression(Compression.SNAPPY)
                .setEnableDebug(false)
                .build();

        server(api);
    }


    public static void server(LibraryImpl api) throws InterruptedException
    {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try
        {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override
                        protected void initChannel(SocketChannel channel)
                        {
                            channel.pipeline().addLast(new HttpServerCodec())
                                    .addLast(new SimpleChannelHandler(api));
                        }
                    });

            ChannelFuture future = bootstrap.bind(8080).sync();
            System.out.println("Server started on port 8080");
            future.channel().closeFuture().sync();
        }
        finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void request(ChannelHandlerContext context, LibraryImpl api)
    {
        long startTime = System.currentTimeMillis();
        final int count = 10000;

        Map<Integer, Response> responseMap = new HashMap<>();

        Map<String, Serializable> map = new HashMap<>();
        map.put("stage", "READY");

        for (int i = 0; i < count; i++)
        {
            int finalI = i;

            api.sendRequest(TEST_QUERY_PREPARED, map).queue(response -> {
                responseMap.put(finalI, response);
                if (responseMap.size() == count)
                {
                    long duration = System.currentTimeMillis() - startTime;
                    System.out.println("Total time: " + duration + " ms");
                    System.out.println("RPS: " + (int) (count * 1000.0 / duration));
                    context.close();
                }
            });
        }
    }

    public static class SimpleChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest>
    {
        private final LibraryImpl api;

        public SimpleChannelHandler(LibraryImpl api)
        {
            this.api = api;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception
        {
            request(context, api);
        }

        @Override
        public boolean isSharable()
        {
            return true;
        }
    }
}
