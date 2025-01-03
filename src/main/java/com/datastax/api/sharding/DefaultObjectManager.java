package com.datastax.api.sharding;

import com.datastax.annotations.Nonnull;
import com.datastax.api.ObjectFactory;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.internal.ObjectFactoryImpl;
import com.datastax.internal.utils.config.ShardingConfig;
import com.datastax.internal.utils.config.ThreadingConfig;
import com.datastax.internal.utils.sharding.ThreadingProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiFunction;

public class DefaultObjectManager implements ObjectManager
{
    public static final Logger LOG = LoggerFactory.getLogger(DefaultObjectManager.class);

    /**
     * The executor that is used by the ShardManager internally to create new JDA instances.
     */
    protected final ScheduledExecutorService executor;

    /**
     * The queue of shards waiting for creation.
     */
    protected final Queue<Integer> queue = new ConcurrentLinkedQueue<>();

    public static final ThreadFactory DEFAULT_THREAD_FACTORY = r ->
    {
        final Thread t = new Thread(r, "DefaultShardManager");
        t.setPriority(Thread.NORM_PRIORITY + 1);
        return t;
    };

    /**
     * The shutdown hook used by this ShardManager. If this is null the shutdown hook is disabled.
     */
    protected final Thread shutdownHook;
    /**
     * The {@link Map} that holds all shards.
     */
    protected Map<Integer, ObjectFactory> shards;

    /**
     * {@link ThreadingProviderConfig} containing a series of {@link ThreadPoolProvider} instances for shard specific configuration.
     */
    protected final ThreadingProviderConfig threadingConfig;
    protected final ShardingConfig shardingConfig;
    protected final BiFunction<String, String, InetSocketAddress> address;

    public DefaultObjectManager(BiFunction<String, String, InetSocketAddress> address, Collection<Integer> shardIds, ThreadingProviderConfig threadingConfig, ShardingConfig shardingConfig)
    {
        this.address = address;
        this.threadingConfig = threadingConfig;
        this.shardingConfig = shardingConfig == null ? ShardingConfig.getDefault() : shardingConfig;
        this.executor = createExecutor(this.threadingConfig.getThreadFactory());
        this.shutdownHook = new Thread(this::shutdown, "Cassandra Shutdown Hook");

        synchronized (queue)
        {
            if (getShardsTotal() != -1)
            {
                if (shardIds == null)
                {
                    this.shards = new HashMap<>(getShardsTotal());
                    for (int i = 0; i < getShardsTotal(); i++)
                        this.queue.add(i);
                }
                else
                {
                    this.shards = new HashMap<>(shardIds.size());

                    shardIds.stream().distinct().sorted().forEach(this.queue::add);
                }
            }
        }
    }

    /**
     * This method creates the internal {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService}.
     * It is intended as a hook for custom implementations to create their own executor.
     *
     * @return A new ScheduledExecutorService
     */
    protected ScheduledExecutorService createExecutor(ThreadFactory threadFactory)
    {
        ThreadFactory factory = threadFactory == null
                ? DEFAULT_THREAD_FACTORY
                : threadFactory;

        return Executors.newSingleThreadScheduledExecutor(factory);
    }

    protected ObjectFactoryImpl buildInstance(Cluster cluster, final int shardId)
    {
        this.retrieveShardTotal(cluster);

        ExecutorPair<ExecutorService> callbackPair = resolveExecutor(threadingConfig.getCallbackPoolProvider(), shardId);
        ExecutorService callbackPool = callbackPair.executor;
        boolean shutdownCallbackPool = callbackPair.automaticShutdown;

        ThreadingConfig threadingConfig = new ThreadingConfig();
        threadingConfig.setCallbackPool(callbackPool, shutdownCallbackPool);

        ObjectFactoryImpl factory = new ObjectFactoryImpl(cluster, threadingConfig);

        final ObjectFactory.ShardInfo shardInfo = new ObjectFactory.ShardInfo(shardId, getShardsTotal());

        factory.setShardManager(this);
        factory.login(shardInfo);

        return factory;
    }

    @Override
    public void login(String username, String password)
    {
        ObjectFactoryImpl factory = null;
        try
        {
            final int shardId = this.queue.isEmpty() ? 0 : this.queue.peek();

            Cluster.Builder builder = Cluster.builder();

            InetSocketAddress address = this.address.apply(username, password);

            builder.withAuthProvider(new PlainTextAuthProvider(username, password));
            builder.addContactPointsWithPorts(address);

            factory = buildInstance(builder.build(), shardId);

            System.out.println(this.shards == null);

            this.shards.put(shardId, factory);
        }
        catch (final Exception e)
        {
            if (factory != null)
            {
                factory.shutdown();
            }

            throw e;
        }

        if (this.shutdownHook != null)
            Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    @Override
    public int getShardsTotal()
    {
        return this.shardingConfig.getShardsTotal();
    }

    @Nonnull
    @Override
    public Map<Integer, ObjectFactory> getShardCache()
    {
        return shards;
    }

    @Override
    public void shutdown()
    {
        if (this.shutdownHook != null)
            Runtime.getRuntime().removeShutdownHook(shutdownHook);

        this.threadingConfig.shutdown();
    }

    private synchronized void retrieveShardTotal(Cluster cluster)
    {
        if (getShardsTotal() != -1)
            return;

        LOG.debug("Fetching shard total");

        int shardTotal = 1;

        this.shardingConfig.setShardsTotal(shardTotal);
        this.shards = new HashMap<>(shardTotal);

        synchronized (queue)
        {
            for (int i = 0; i < shardTotal; i++)
                queue.add(i);
        }
	}

    protected static <E extends ExecutorService> ExecutorPair<E> resolveExecutor(ThreadPoolProvider<? extends E> provider, int shardId)
    {
        E executor = null;
        boolean automaticShutdown = true;
        if (provider != null)
        {
            executor = provider.provide(shardId);
            automaticShutdown = provider.shouldShutdownAutomatically(shardId);
        }
        return new ExecutorPair<>(executor, automaticShutdown);
    }

    protected static class ExecutorPair<E extends ExecutorService>
    {
        protected final E executor;
        protected final boolean automaticShutdown;

        protected ExecutorPair(E executor, boolean automaticShutdown)
        {
            this.executor = executor;
            this.automaticShutdown = automaticShutdown;
        }
    }
}
