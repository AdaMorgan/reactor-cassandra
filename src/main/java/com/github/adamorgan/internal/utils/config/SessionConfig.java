package com.github.adamorgan.internal.utils.config;

import com.github.adamorgan.api.utils.ConcurrentSessionController;
import com.github.adamorgan.api.utils.SessionController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SessionConfig
{
    protected final SessionController controller;
    protected final int maxReconnectDelay;

    public SessionConfig(@Nullable SessionController controller, int maxReconnectDelay)
    {
        this.controller = controller == null ? new ConcurrentSessionController() : controller;
        this.maxReconnectDelay = maxReconnectDelay;
    }

    public int getMaxReconnectDelay()
    {
        return maxReconnectDelay;
    }

    @Nonnull
    public SessionController getSessionController()
    {
        return controller;
    }
}
