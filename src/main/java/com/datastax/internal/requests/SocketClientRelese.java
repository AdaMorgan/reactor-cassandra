package com.datastax.internal.requests;

import com.datastax.api.Library;
import com.datastax.api.utils.SessionController;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class SocketClientRelese
{
    public static final ThreadLocal<ByteBuf> CURRENT_EVENT = new ThreadLocal<>();

    public abstract static class ConnectNode implements SessionController.SessionConnectNode
    {
        protected final Library api;

        protected final byte version;
        protected final byte flags;
        protected final byte opcode;

        public ConnectNode(Library api, byte version, byte flags, byte opcode)
        {
            this.api = api;
            this.version = version;
            this.flags = flags;
            this.opcode = opcode;
        }

        @Override
        public Library getLibrary()
        {
            return api;
        }

        @Override
        public ByteBuf asByteBuf()
        {
            return new EntityBuilder()
                    .writeByte(this.version)
                    .writeByte(this.flags)
                    .writeShort(0x00)
                    .writeByte(this.opcode)
                    .asByteBuf();
        }

        @Override
        public String toString()
        {
            return ByteBufUtil.prettyHexDump(asByteBuf());
        }
    }

    public static class StartingNode extends ConnectNode
    {
        public StartingNode(Library api, byte version, byte flags)
        {
            super(api, version, flags, SocketCode.STARTUP);
        }

        @Override
        public boolean isReconnect()
        {
            return false;
        }

        @Override
        public ByteBuf asByteBuf()
        {
            return super.asByteBuf();
        }
    }
}
