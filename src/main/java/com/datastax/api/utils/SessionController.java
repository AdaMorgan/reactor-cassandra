package com.datastax.api.utils;

import com.datastax.api.Library;
import io.netty.buffer.ByteBuf;

public interface SessionController
{
    interface SessionConnectNode
    {
        /**
         * Whether this node is reconnecting. Can be used to setup a priority based system.
         *
         * @return True, if this session is reconnecting
         */
        boolean isReconnect();

        Library getLibrary();

        ByteBuf asByteBuf();
    }
}
