package com.datastax.api.hooks;

import com.datastax.api.events.GenericEvent;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface EventListener
{
    void onEvent(@Nonnull GenericEvent event);
}
