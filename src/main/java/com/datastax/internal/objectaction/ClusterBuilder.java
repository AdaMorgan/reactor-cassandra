package com.datastax.internal.objectaction;

import com.datastax.annotations.Nonnull;
import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.RetryPolicy;

public class ClusterBuilder {
    private final AuthProvider provider;
    private PoolingOptions option;
    private Cluster.Builder builder;

    public final static Cluster DEFAULT = new Cluster.Builder()
            .addContactPoint("127.0.0.1")
            .withPort(9042)
            .build();

    private ClusterBuilder(String username, String password) {
        this.provider = new PlainTextAuthProvider(username, password);
        this.builder = Cluster.builder().withAuthProvider(this.provider);
        this.option = new PoolingOptions();
    }

    @Nonnull
    public static ClusterBuilder create(String username, String password) {
        return new ClusterBuilder(username, password);
    }

    @Nonnull
    public ClusterBuilder withCompression(ProtocolOptions.Compression compression) {
        this.builder = this.builder.withCompression(compression);
        return this;
    }

    @Nonnull
    public ClusterBuilder withClusterName(String name) {
        DCAwareRoundRobinPolicy policy = DCAwareRoundRobinPolicy.builder()
                .withLocalDc(name)
                .build();
        this.builder = this.builder.withClusterName(name).withLoadBalancingPolicy(policy);
        return this;
    }

    @Nonnull
    public ClusterBuilder withHost(String host) {
        this.builder = this.builder.addContactPoint(host);
        return this;
    }

    @Nonnull
    public ClusterBuilder withPort(int port) {
        this.builder = this.builder.withPort(port);
        return this;
    }

    @Nonnull
    public ClusterBuilder withNettyOptions(NettyOptions options) {
        this.builder = this.builder.withNettyOptions(options);
        return this;
    }

    @Nonnull
    public ClusterBuilder withoutMetrics() {
        this.builder = this.builder.withoutMetrics();
        return this;
    }

    @Nonnull
    public ClusterBuilder withRetryPolicy(RetryPolicy policy) {
        this.builder = this.builder.withRetryPolicy(policy);
        return this;
    }

    @Nonnull
    public ClusterBuilder withoutJMXReporting() {
        this.builder = this.builder.withoutJMXReporting();
        return this;
    }

    @Nonnull
    public ClusterBuilder setMaxRequestsPerConnection(HostDistance distance, int newMaxRequests) {
        this.option = this.option.setMaxRequestsPerConnection(distance, newMaxRequests);
        return this;
    }

    @Nonnull
    public ClusterBuilder withHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.option = this.option.setHeartbeatIntervalSeconds(heartbeatIntervalSeconds);
        return this;
    }

    @Nonnull
    public ClusterBuilder withMaxSchemaAgreement(int millis) {
        this.builder = this.builder.withMaxSchemaAgreementWaitSeconds(millis / 1000);
        return this;
    }

    public Cluster build() {
        return this.builder.withProtocolVersion(ProtocolVersion.V4)
                .withPoolingOptions(this.option)
                .withoutJMXReporting()
                .build();
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
