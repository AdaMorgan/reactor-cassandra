package com.datastax.api.sharding;

import com.datastax.annotations.Nonnull;
import com.datastax.annotations.Nullable;
import com.datastax.api.ObjectFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ObjectManager {

    void login(String username, String password);

    int getShardsTotal();

    @Nonnull
    Map<Integer, ObjectFactory> getShardCache();

    @Nonnull
    default List<ObjectFactory> getShards()
    {
        return new ArrayList<>(this.getShardCache().values());
    }

    @Nullable
    default ObjectFactory getShardById(final int id)
    {
        return this.getShardCache().get(id);
    }

    void shutdown();
}
