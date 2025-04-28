package com.github.adamorgan.internal.utils.config;

import com.github.adamorgan.api.utils.ConcurrentSessionController;
import com.github.adamorgan.api.utils.SessionController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SessionConfig
{
    protected final SessionController controller;
    protected final int maxBufferSize;
    protected final int maxReconnectDelay;

    public SessionConfig(@Nullable SessionController controller, int maxBufferSize, int maxReconnectDelay)
    {
        this.controller = controller == null ? new ConcurrentSessionController() : controller;
        this.maxBufferSize = maxBufferSize;
        this.maxReconnectDelay = maxReconnectDelay;
    }

    @Nonnull
    public SessionController getSessionController()
    {
        return controller;
    }

    public int getMaxBufferSize()
    {
        return maxBufferSize;
    }

    public int getMaxReconnectDelay()
    {
        return maxReconnectDelay;
    }
}
