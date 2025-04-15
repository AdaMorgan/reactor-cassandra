package com.datastax.api.utils;

import com.datastax.api.Library;
import io.netty.buffer.ByteBuf;

public interface SessionController
{
    interface SessionConnectNode
    {
        Library getLibrary();

        ByteBuf asByteBuf();
    }
}
