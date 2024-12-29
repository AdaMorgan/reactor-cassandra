package com.datastax.api;

import com.datastax.annotations.Nonnull;

import java.util.concurrent.ExecutorService;

public interface ObjectFactory {

    @Nonnull
    ExecutorService getCallbackPool();

    void shutdown();
}
