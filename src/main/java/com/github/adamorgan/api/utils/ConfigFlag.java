package com.github.adamorgan.api.utils;

import java.util.EnumSet;
import java.util.stream.Collectors;

public enum ConfigFlag
{
    EVENT_PASSTHROUGH,
    SHUTDOWN_HOOK(true),
    DEBUG(true),
    AUTO_RECONNECT(true);

    private final boolean isDefault;

    public static final EnumSet<ConfigFlag> DEFAULT = EnumSet.allOf(ConfigFlag.class).stream().filter(ConfigFlag::isDefault).collect(Collectors.toCollection(() -> EnumSet.noneOf(ConfigFlag.class)));

    ConfigFlag()
    {
        this(false);
    }

    ConfigFlag(boolean isDefault)
    {
        this.isDefault = isDefault;
    }

    public boolean isDefault()
    {
        return isDefault;
    }
}
