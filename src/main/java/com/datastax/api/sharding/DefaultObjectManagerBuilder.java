package com.datastax.api.sharding;

import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;
import com.datastax.api.ObjectFactory;
import com.datastax.internal.utils.Checks;
import com.datastax.internal.utils.config.ShardingConfig;
import com.datastax.internal.utils.sharding.ThreadingProviderConfig;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;

public class DefaultObjectManagerBuilder
{
    private final String username;
    private final String password;
    protected Collection<Integer> shards = null;
    protected final BiFunction<String, String, InetSocketAddress> address;

    protected ThreadPoolProvider<? extends ExecutorService> callbackPoolProvider;
    protected ThreadFactory threadFactory;
    protected int shardsTotal = -1;

    private DefaultObjectManagerBuilder(String username, String password, BiFunction<String, String, InetSocketAddress> address)
    {
        this.username = username;
        this.password = password;
        this.address = address;
    }

    public static DefaultObjectManagerBuilder create(String username, String password, BiFunction<String, String, InetSocketAddress> address)
    {
        return new DefaultObjectManagerBuilder(username, password, address);
    }

    /**
     * Sets the {@link java.util.concurrent.ThreadFactory ThreadFactory} that will be used by the internal executor
     * of the ShardManager.
     * <p>Note: This will not affect Threads created by any JDA instance.
     *
     * @param  threadFactory
     *         The ThreadFactory or {@code null} to reset to the default value.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultObjectManagerBuilder setThreadFactory(@Nullable final ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
        return this;
    }

    /**
     * Sets the {@link ExecutorService ExecutorService} that should be used in
     * the JDA callback handler which mostly consists of {@link com.datastax.api.request.ObjectAction ObjectAction} callbacks.
     * By default JDA will use {@link ForkJoinPool#commonPool()}
     * <br><b>Only change this pool if you know what you're doing.</b>
     *
     * <p>This is used to handle callbacks of {@link com.datastax.api.request.ObjectAction#queue() ObjectAction#queue()}, similarly it is used to
     * finish {@link com.datastax.api.request.ObjectAction#submit() ObjectAction#submit()} tasks which build on queue.
     *
     * <p>Default: {@link ForkJoinPool#commonPool()}
     *
     * @param  executor
     *         The thread-pool to use for callback handling
     * @param  automaticShutdown
     *         Whether {@link ObjectFactory#shutdown()} should automatically shutdown this pool
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultObjectManagerBuilder setCallbackPool(@Nullable ExecutorService executor, boolean automaticShutdown)
    {
        return setCallbackPoolProvider(executor == null ? null : new ThreadPoolProviderImpl<>(executor, automaticShutdown));
    }

    /**
     * Sets the {@link ExecutorService ExecutorService} that should be used in
     * the JDA callback handler which mostly consists of {@link com.datastax.api.request.ObjectAction ObjectAction} callbacks.
     * By default JDA will use {@link ForkJoinPool#commonPool()}
     * <br><b>Only change this pool if you know what you're doing.</b>
     *
     * <p>This is used to handle callbacks of {@link com.datastax.api.request.ObjectAction#queue() ObjectAction#queue()}, similarly it is used to
     * finish {@link com.datastax.api.request.ObjectAction#submit() ObjectAction#submit()} tasks which build on queue.
     *
     * <p>Default: {@link ForkJoinPool#commonPool()}
     *
     * @param  provider
     *         The thread-pool provider to use for callback handling
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultObjectManagerBuilder setCallbackPoolProvider(@Nullable ThreadPoolProvider<? extends ExecutorService> provider)
    {
        this.callbackPoolProvider = provider;
        return this;
    }

    public ObjectManager build()
    {
        return build(true);
    }

    /**
     * Sets the range of shards the {@link DefaultObjectManager DefaultObjectManager} should contain.
     * This is useful if you want to split your shards between multiple JVMs or servers.
     *
     * <p><b>This does not have any effect if the total shard count is set to {@code -1} (get recommended shards from discord).</b>
     *
     * @param  minShardId
     *         The lowest shard id the DefaultShardManager should contain
     *
     * @param  maxShardId
     *         The highest shard id the DefaultShardManager should contain
     *
     * @throws IllegalArgumentException
     *         If either minShardId is negative, maxShardId is lower than shardsTotal or
     *         minShardId is lower than or equal to maxShardId
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultObjectManagerBuilder setShards(final int minShardId, final int maxShardId)
    {
        Checks.notNegative(minShardId, "minShardId");
        Checks.check(maxShardId < this.shardsTotal, "maxShardId must be lower than shardsTotal");
        Checks.check(minShardId <= maxShardId, "minShardId must be lower than or equal to maxShardId");

        List<Integer> shards = new ArrayList<>(maxShardId - minShardId + 1);
        for (int i = minShardId; i <= maxShardId; i++)
            shards.add(i);

        this.shards = shards;

        return this;
    }

    /**
     * This will set the total amount of shards the {@link DefaultObjectManager DefaultObjectManager} should use.
     * <p> If this is set to {@code -1} JDA will automatically retrieve the recommended amount of shards from discord (default behavior).
     *
     * @param  shardsTotal
     *         The number of overall shards or {@code -1} if JDA should use the recommended amount from discord.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    #setShards(int, int)
     */
    @Nonnull
    public DefaultObjectManagerBuilder setShardsTotal(final int shardsTotal)
    {
        Checks.check(shardsTotal == -1 || shardsTotal > 0, "shardsTotal must either be -1 or greater than 0");
        this.shardsTotal = shardsTotal;

        return this;
    }

    public ObjectManager build(boolean login)
    {
        final ShardingConfig shardingConfig = new ShardingConfig(this.shardsTotal, false);
        final ThreadingProviderConfig threadingConfig = new ThreadingProviderConfig(this.threadFactory, this.callbackPoolProvider);

        DefaultObjectManager manager = new DefaultObjectManager(this.address, this.shards, threadingConfig, shardingConfig);

        if (login)
            manager.login(this.username, this.password);

        return manager;
    }

    public static final class ThreadPoolProviderImpl<T extends ExecutorService> implements ThreadPoolProvider<T>
    {
        private final boolean autoShutdown;
        private final T pool;

        public ThreadPoolProviderImpl(T pool, boolean autoShutdown)
        {
            this.autoShutdown = autoShutdown;
            this.pool = pool;
        }

        @Override
        public T provide(int shardId)
        {
            return pool;
        }

        @Override
        public boolean shouldShutdownAutomatically(int shardId)
        {
            return autoShutdown;
        }
    }
}
