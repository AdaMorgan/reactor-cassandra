package com.github.adamorgan.api.utils;

import com.github.adamorgan.api.Library;

import javax.annotation.Nonnull;

public interface SessionController
{
    void appendSession(@Nonnull SessionConnectNode node);

    void removeSession(@Nonnull SessionConnectNode node);

    interface SessionConnectNode
    {
        @Nonnull
        Library getLibrary();
    }
}
