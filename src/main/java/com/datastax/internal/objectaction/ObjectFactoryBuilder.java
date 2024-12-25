package com.datastax.internal.objectaction;

import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;
import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.RetryPolicy;
import com.datastax.internal.utils.config.ThreadingConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public class ObjectFactoryBuilder {
    private final AuthProvider provider;
    private PoolingOptions option;
    private Cluster.Builder builder;

    private ExecutorService callbackPool;
    private boolean shutdownCallbackPool;

    private ObjectFactoryBuilder(String username, String password) {
        this.provider = new PlainTextAuthProvider(username, password);
        this.builder = Cluster.builder().withAuthProvider(this.provider);
        this.option = new PoolingOptions();
    }

    @Nonnull
    public static ObjectFactoryBuilder create(String username, String password) {
        return new ObjectFactoryBuilder(username, password);
    }

    @Nonnull
    public ObjectFactoryBuilder withCompression(ProtocolOptions.Compression compression) {
        this.builder = this.builder.withCompression(compression);
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withClusterName(String name) {
        DCAwareRoundRobinPolicy policy = DCAwareRoundRobinPolicy.builder()
                .withLocalDc(name)
                .build();
        this.builder = this.builder.withClusterName(name).withLoadBalancingPolicy(policy);
        return this;
    }


    /**
     * Sets the {@link ExecutorService ExecutorService} that should be used in
     * the JDA callback handler which mostly consists of {@link ObjectAction ObjectAction} callbacks.
     * By default JDA will use {@link ForkJoinPool#commonPool()}
     * <br><b>Only change this pool if you know what you're doing.
     * <br>This automatically disables the automatic shutdown of the callback pool, you can enable
     * it using {@link #setCallbackPool(ExecutorService, boolean) setCallbackPool(executor, true)}</b>
     *
     * <p>This is used to handle callbacks of {@link ObjectAction#queue()}, similarly it is used to
     * finish {@link ObjectAction#submit()} and {@link ObjectAction#complete()} tasks which build on queue.
     *
     * <p>Default: {@link ForkJoinPool#commonPool()}
     *
     * @param  executor
     *         The thread-pool to use for callback handling
     *
     * @return The JDABuilder instance. Useful for chaining.
     */
    @Nonnull
    public ObjectFactoryBuilder setCallbackPool(@Nullable ExecutorService executor)
    {
        return setCallbackPool(executor, executor == null);
    }

    /**
     * Sets the {@link ExecutorService ExecutorService} that should be used in
     * the JDA callback handler which mostly consists of {@link ObjectAction ObjectAction} callbacks.
     * By default JDA will use {@link ForkJoinPool#commonPool()}
     * <br><b>Only change this pool if you know what you're doing.</b>
     *
     * <p>This is used to handle callbacks of {@link ObjectAction#queue()}, similarly it is used to
     * finish {@link ObjectAction#submit()} and {@link ObjectAction#complete()} tasks which build on queue.
     *
     * <p>Default: {@link ForkJoinPool#commonPool()}
     *
     * @param  executor
     *         The thread-pool to use for callback handling
     * @param  automaticShutdown
     *         Whether {@link ObjectFactory#shutdown()} should shutdown this executor
     *
     * @return The JDABuilder instance. Useful for chaining.
     */
    @Nonnull
    public ObjectFactoryBuilder setCallbackPool(@Nullable ExecutorService executor, boolean automaticShutdown)
    {
        this.callbackPool = executor;
        this.shutdownCallbackPool = automaticShutdown;
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withHost(String host) {
        this.builder = this.builder.addContactPoint(host);
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withPort(int port) {
        this.builder = this.builder.withPort(port);
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withNettyOptions(NettyOptions options) {
        this.builder = this.builder.withNettyOptions(options);
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withoutMetrics() {
        this.builder = this.builder.withoutMetrics();
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withRetryPolicy(RetryPolicy policy) {
        this.builder = this.builder.withRetryPolicy(policy);
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withoutJMXReporting() {
        this.builder = this.builder.withoutJMXReporting();
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder setMaxRequestsPerConnection(HostDistance distance, int newMaxRequests) {
        this.option = this.option.setMaxRequestsPerConnection(distance, newMaxRequests);
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.option = this.option.setHeartbeatIntervalSeconds(heartbeatIntervalSeconds);
        return this;
    }

    @Nonnull
    public ObjectFactoryBuilder withMaxSchemaAgreement(int millis) {
        this.builder = this.builder.withMaxSchemaAgreementWaitSeconds(millis / 1000);
        return this;
    }

    public ObjectFactoryImpl build() {
        Cluster cluster = this.builder.withProtocolVersion(ProtocolVersion.V4).withPoolingOptions(this.option).build();

        ThreadingConfig threadingConfig = ThreadingConfig.getDefault();
        threadingConfig.setCallbackPool(callbackPool, shutdownCallbackPool);

        return new ObjectFactoryImpl(cluster, threadingConfig);
    }

    public static class DefaultLatencyTracker implements LatencyTracker {

        @Override
        public void update(Host host, Statement statement, Exception exception, long newLatencyNanos) {

        }

        @Override
        public void onRegister(Cluster cluster) {

        }

        @Override
        public void onUnregister(Cluster cluster) {

        }
    }
}
