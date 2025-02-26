package com.datastax.api;

import java.util.concurrent.ExecutorService;

public interface ObjectFactory {

    ExecutorService getCallbackPool();

    void shutdown();
}
