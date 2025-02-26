package com.datastax.api.hooks;

import com.datastax.api.events.GenericEvent;
import com.datastax.api.events.session.ReadyEvent;
import com.datastax.api.events.session.SessionDisconnectEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class ListenerAdapter implements EventListener
{
    public void onGenericEvent(@Nonnull GenericEvent event) {}

    public void onReady(@Nonnull ReadyEvent event) {}
    public void onSessionDisconnect(@Nonnull SessionDisconnectEvent event) {}

    @Override
    public final void onEvent(@NotNull GenericEvent event)
    {
        onGenericEvent(event);
    }
}
