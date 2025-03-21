package com.datastax.internal.requests;

import com.datastax.api.Library;
import com.datastax.api.utils.SessionController;
import com.datastax.internal.entities.EntityBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class SocketClient
{
    protected abstract class ConnectNode implements SessionController.SessionConnectNode
    {
        protected final Library api;
        protected final EntityBuilder entityBuilder;

        protected final byte version;
        protected final byte flags;
        protected final short stream;
        protected final byte opcode;

        public ConnectNode(Library api, byte version, byte flags, short stream, byte opcode)
        {
            this.api = api;
            this.version = version;
            this.flags = flags;
            this.stream = stream;
            this.opcode = opcode;
            this.entityBuilder = new EntityBuilder();
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
                    .writeShort(this.stream)
                    .writeByte(this.opcode)
                    .asByteBuf();
        }

        @Override
        public String toString()
        {
            return ByteBufUtil.prettyHexDump(asByteBuf());
        }
    }

    public class StartingNode extends ConnectNode
    {

        public StartingNode(Library api, byte version, byte flags, short stream)
        {
            super(api, version, flags, stream, SocketCode.STARTUP);
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
