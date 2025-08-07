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

import io.netty.channel.AbstractEventLoop;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadStreamExecutor extends AbstractEventLoop implements ExecutorService
{
    @Override
    public ChannelFuture register(Channel channel)
    {
        return null;
    }

    @Override
    public ChannelFuture register(ChannelPromise promise)
    {
        return null;
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise)
    {
        return null;
    }

    @Override
    public boolean isShuttingDown()
    {
        return false;
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit)
    {
        return null;
    }

    @Override
    public Future<?> terminationFuture()
    {
        return null;
    }

    @Override
    public void shutdown()
    {

    }

    @Override
    public boolean isShutdown()
    {
        return false;
    }

    @Override
    public boolean isTerminated()
    {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException
    {
        return false;
    }

    @Override
    public boolean inEventLoop(Thread thread)
    {
        return false;
    }

    @Override
    public void execute(@Nonnull Runnable command)
    {

    }
}
