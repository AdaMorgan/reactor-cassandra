package com.datastax.api.sharding;

import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.function.IntFunction;

/**
 * Called by {@link DefaultObjectManager} when building a JDA instance.
 * <br>Every time a JDA instance is built, the manager will first call {@link #provide(int)} followed by
 * a call to {@link #shouldShutdownAutomatically(int)}.
 *
 * @param <T>
 *        The type of executor
 */
public interface ThreadPoolProvider<T>
{
    /**
     * Provides an instance of the specified executor, or null
     *
     * @param  shardId
     *         The current shard id
     *
     * @return The Executor Service
     */
    @Nullable
    T provide(int shardId);

    /**
     * Whether the previously provided executor should be shutdown by {@link com.datastax.api.ObjectFactory#shutdown() ObjectFactory#shutdown()}.
     *
     * @param  shardId
     *         The current shard id
     *
     * @return True, if the executor should be shutdown by JDA
     */
    default boolean shouldShutdownAutomatically(int shardId)
    {
        return false;
    }

    /**
     * Provider that initializes with a {@link DefaultObjectManagerBuilder#setShardsTotal(int) shard_total}
     * and provides the same pool to share between shards.
     *
     * @param  init
     *         Function to initialize the shared pool, called with the shard total
     *
     * @param  <T>
     *         The type of executor
     *
     * @return The lazy pool provider
     */
    @Nonnull
    static <T extends ExecutorService> LazySharedProvider<T> lazy(@Nonnull IntFunction<T> init)
    {
        return new LazySharedProvider<>(init);
    }

    final class LazySharedProvider<T extends ExecutorService> implements ThreadPoolProvider<T>
    {
        private final IntFunction<T> initializer;
        private T pool;

        LazySharedProvider(@Nonnull IntFunction<T> initializer)
        {
            this.initializer = initializer;
        }

        /**
         * Called with the shard total to initialize the shared pool.
         *
         * <p>This also destroys the temporary pool created for fetching the recommended shard total.
         *
         * @param shardTotal
         *        The shard total
         */
        public synchronized void init(int shardTotal)
        {
            if (pool == null)
                pool = initializer.apply(shardTotal);
        }

        /**
         * Shuts down the shared pool and the temporary pool.
         */
        public synchronized void shutdown()
        {
            if (pool != null)
            {
                pool.shutdown();
                pool = null;
            }
        }

        /**
         * Provides the initialized pool or the temporary pool if not initialized yet.
         *
         * @param  shardId
         *         The current shard id
         *
         * @return The thread pool instance
         */
        @Nullable
        @Override
        public synchronized T provide(int shardId)
        {
            return pool;
        }
    }
}
