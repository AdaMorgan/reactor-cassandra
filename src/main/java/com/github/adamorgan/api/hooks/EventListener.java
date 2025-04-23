package com.github.adamorgan.api.hooks;

import com.github.adamorgan.api.events.GenericEvent;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface EventListener
{
    void onEvent(@Nonnull GenericEvent event);
}
