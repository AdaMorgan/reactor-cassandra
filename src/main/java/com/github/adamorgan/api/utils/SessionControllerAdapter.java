package com.github.adamorgan.api.utils;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SessionControllerAdapter implements SessionController
{
    private final ConcurrentLinkedQueue<SessionConnectNode> connectQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void appendSession(@Nonnull SessionConnectNode node)
    {
        this.connectQueue.add(node);
    }

    @Override
    public void removeSession(@Nonnull SessionConnectNode node)
    {
        this.connectQueue.remove(node);
    }
}
