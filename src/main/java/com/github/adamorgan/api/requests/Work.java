package com.github.adamorgan.api.requests;

import com.github.adamorgan.api.Library;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public interface Work
{
    @Nonnull
    Library getLibrary();

    void execute();

    @Nonnull
    ByteBuf getBody();

    boolean isSkipped();

    boolean isDone();

    boolean isCancelled();

    void cancel();
}
