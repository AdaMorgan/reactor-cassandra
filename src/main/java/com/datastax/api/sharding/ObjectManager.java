package com.datastax.api.sharding;

import com.datastax.annotations.Nonnull;
import com.datastax.api.ObjectFactory;

import java.util.Map;

public interface ObjectManager {

    void login(String username, String password);

    @Nonnull
    Map<Integer, ObjectFactory> getShards();

    void shutdown();
}
