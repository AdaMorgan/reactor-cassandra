package com.datastax.internal.objectaction;

import java.util.concurrent.CompletableFuture;

public interface ObjectAction<T> {

    String getRoute();

    void queue();

    void complete();

    CompletableFuture<T> submit();
}
