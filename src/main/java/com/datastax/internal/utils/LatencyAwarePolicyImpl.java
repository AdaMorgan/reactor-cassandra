package com.datastax.internal.utils;

import com.datastax.driver.core.policies.LatencyAwarePolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;

import java.util.function.Supplier;

public class LatencyAwarePolicyImpl extends TokenAwarePolicy implements LoadBalancingPolicy {
    private final LatencyAwarePolicy.Builder latencyAwatePolicy;

    public LatencyAwarePolicyImpl(Supplier<LatencyAwarePolicy> childPolicy) {
        this(childPolicy, ReplicaOrdering.RANDOM);
    }

    public LatencyAwarePolicyImpl(Supplier<LatencyAwarePolicy> childPolicy, ReplicaOrdering replicaOrdering) {
        this(childPolicy.get(), replicaOrdering);
    }

    private LatencyAwarePolicyImpl(LoadBalancingPolicy childPolicy, ReplicaOrdering replicaOrdering) {
        super(childPolicy, replicaOrdering);
        this.latencyAwatePolicy = new LatencyAwarePolicy.Builder(this);
    }
}
