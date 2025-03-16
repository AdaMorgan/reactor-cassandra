package com.datastax.api.hooks;

import com.datastax.api.events.GenericEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class ListenerAdapter implements EventListener
{
    public void onGenericEvent(@Nonnull GenericEvent event) { }


    @Override
    public void onEvent(@NotNull GenericEvent event)
    {

    }
}
