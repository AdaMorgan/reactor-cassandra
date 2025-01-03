package com.datastax.internal.utils.config;

import com.datastax.annotations.Nonnull;

public class ShardingConfig
{
    private int shardsTotal;
    private final boolean useShutdownNow;

    public ShardingConfig(int shardsTotal, boolean useShutdownNow)
    {
        this.shardsTotal = shardsTotal;
        this.useShutdownNow = useShutdownNow;
    }

    public void setShardsTotal(int shardsTotal)
    {
        this.shardsTotal = shardsTotal;
    }

    public int getShardsTotal()
    {
        return shardsTotal;
    }

    public boolean isUseShutdownNow()
    {
        return useShutdownNow;
    }

    @Nonnull
    public static ShardingConfig getDefault()
    {
        return new ShardingConfig(1, false);
    }
}
