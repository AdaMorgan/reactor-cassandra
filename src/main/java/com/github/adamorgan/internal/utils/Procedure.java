package com.github.adamorgan.internal.utils;

import com.github.adamorgan.api.utils.request.ObjectRequest;
import io.netty.buffer.ByteBuf;

/**
 * Iteration procedure accepting one argument and returning whether to continue iteration.
 *
 * @param <T> The type of the argument
 */
@FunctionalInterface
public interface Procedure<T extends ObjectRequest<T>>
{
    ByteBuf apply(ObjectRequest<T> objAction, byte version, byte flags, short stream);
}
