package com.github.adamorgan.api.requests;

import com.github.adamorgan.internal.requests.action.ObjectActionImpl;

@FunctionalInterface
interface Header<T>
{
    void apply(ObjectActionImpl<T> restAction, byte version, byte flags, short stream, byte opcode);
}
