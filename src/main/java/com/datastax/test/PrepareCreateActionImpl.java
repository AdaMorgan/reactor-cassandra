package com.datastax.test;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.utils.data.DataType;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class PrepareCreateActionImpl extends ObjectCreateActionImpl
{
    public PrepareCreateActionImpl(LibraryImpl api, byte flags, String content, @Nonnull Consistency consistency, ObjectFlags... queryFlags)
    {
        super(api, flags, SocketCode.PREPARE, content, consistency.getCode(), queryFlags);
    }

    @Override
    public void handleSuccess(@Nonnull Request<ByteBuf> request, @Nonnull Response response)
    {
        new ExecuteActionImpl(this.api, flags, response.getBody(), Consistency.ONE, ObjectFlags.VALUES, ObjectFlags.PAGE_SIZE, ObjectFlags.DEFAULT_TIMESTAMP).queue(request::onSuccess, request::onFailure);
    }

    private static final class ExecuteActionImpl extends ObjectActionImpl<ByteBuf>
    {
        private final ByteBuf body;
        private final int level;
        private final int executeFlags;

        public ExecuteActionImpl(LibraryImpl api, byte flags, ByteBuf body, Consistency consistency, ObjectFlags... executeFlags)
        {
            super(api, flags, SocketCode.EXECUTE);
            this.body = body;
            this.level = consistency.getCode();
            this.executeFlags = Arrays.stream(executeFlags).mapToInt(ObjectFlags::getValue).reduce(0, ((result, original) -> result | original));
        }

        @Override
        protected void handleSuccess(Request<ByteBuf> request, Response response)
        {
            request.onSuccess(response.getBody());
        }

        public ByteBuf execute(ByteBuf buffer)
        {
            int kind = buffer.readInt();

            int idLength = buffer.readUnsignedShort();
            byte[] preparedId = new byte[idLength];
            buffer.readBytes(preparedId);

            ByteBuf buf = Unpooled.directBuffer()
                    .writeByte(this.version)
                    .writeByte(this.flags)
                    .writeShort(0x03)
                    .writeByte(this.opcode);

            ByteBuf body = Unpooled.directBuffer();

            body.writeShort(preparedId.length);
            body.writeBytes(preparedId);

            body.writeShort(this.level);

            body.writeByte(this.executeFlags);

            //--- size
            body.writeShort(2);

            //--- 1
            writeLongValue(body, 123456L);

            //--- 2
            DataType.LONG_STRING.encode(body, "user");

            //--- flags
            body.writeInt(5000); // page size
            body.writeLong(System.currentTimeMillis()); // timestamp

            buf.writeInt(body.readableBytes());
            buf.writeBytes(body);

            return buf;
        }

        @Nonnull
        @Override
        public ByteBuf asByteBuf()
        {
            return execute(body);
        }

        public ByteBuf executeParameters(ByteBuf buffer)
        {
            int idLength = buffer.readUnsignedShort();
            byte[] preparedId = new byte[idLength];
            buffer.readBytes(preparedId);

            ByteBuf buf = Unpooled.directBuffer()
                    .writeByte(this.version)
                    .writeByte(this.flags)
                    .writeShort(0x03)
                    .writeByte(this.opcode);

            ByteBuf body = Unpooled.directBuffer();

            body.writeShort(preparedId.length);
            body.writeBytes(preparedId);

            body.writeShort(this.level);

            body.writeByte(this.executeFlags);

            body.writeShort(2);

            DataType.STRING.encode(body, "user_id");
            writeLongValue(body, 123456L);

            DataType.STRING.encode(body, "user_name");
            DataType.LONG_STRING.encode(body, "user");

            body.writeInt(5000);
            body.writeLong(1743025467097000L);

            buf.writeInt(body.readableBytes());
            buf.writeBytes(body);

            return buf;
        }

        private void writeLongValue(ByteBuf buf, long value)
        {
            buf.writeInt(8);
            buf.writeLong(value);
        }
    }
}
