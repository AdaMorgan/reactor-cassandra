package com.github.adamorgan.api.requests;

import io.netty.buffer.ByteBuf;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.annotation.Nonnull;

public interface Work
{
    @Nonnull
    CaseInsensitiveMap<String, Integer> getHeaders();

    @Nonnull
    ByteBuf getBody();
}
