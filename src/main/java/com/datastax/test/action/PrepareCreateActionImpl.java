package com.datastax.test.action;

import com.datastax.api.requests.ObjectAction;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PrepareCreateActionImpl extends ObjectCreateActionImpl
{
    public PrepareCreateActionImpl(LibraryImpl api, byte version, byte flags, String content, Level level, Flag... queryFlags)
    {
        super(api, version, flags, SocketCode.PREPARE, content, level, queryFlags);
    }

    @Override
    public void handleSuccess(Request<ByteBuf> request, Response response)
    {
        new ExecuteActionImpl(this.api, this.version, flags, response.getBody(), Level.ONE, ObjectAction.Flag.VALUES, ObjectAction.Flag.PAGE_SIZE, ObjectAction.Flag.DEFAULT_TIMESTAMP).queue(request::onSuccess, request::onFailure);
    }

    private final class ExecuteActionImpl extends ObjectActionImpl<ByteBuf>
    {
        private final ByteBuf body;
        private final int level;
        private final int executeFlags;

        public ExecuteActionImpl(LibraryImpl api, byte version, byte flags, ByteBuf body, Level level, Flag... executeFlags)
        {
            super(api, version, flags, SocketCode.EXECUTE);
            this.body = body;
            this.level = level.getCode();
            this.executeFlags = Arrays.stream(executeFlags).mapToInt(Flag::getValue).reduce(0, ((result, original) -> result | original));
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
                    .writeShort(0x00)
                    .writeByte(this.opcode);

            ByteBuf body = Unpooled.directBuffer();

            body.writeShort(preparedId.length);
            body.writeBytes(preparedId);

            body.writeShort(this.level);

            body.writeByte(this.executeFlags);

            //----
            body.writeShort(2);
            //----
            writeLongValue(body, 123456);
            writeString(body, "user", EntityBuilder.TypeTag.INT);
            //----
            body.writeInt(5000);
            body.writeLong(1743025467097000L);
            //----

            buf.writeInt(body.readableBytes());
            buf.writeBytes(body);

            return buf;
        }

        @Nonnull
        @Override
        public ByteBuf applyData()
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
                    .writeShort(0x00)
                    .writeByte(this.opcode);

            ByteBuf body = Unpooled.directBuffer();

            body.writeShort(preparedId.length);
            body.writeBytes(preparedId);

            body.writeShort(this.level);

            body.writeByte(this.executeFlags);

            body.writeShort(2);

            writeString(body, "user_id", EntityBuilder.TypeTag.SHORT);
            writeLongValue(body, 123456L);

            writeString(body, "user_name", EntityBuilder.TypeTag.SHORT);
            writeString(body, "user", EntityBuilder.TypeTag.INT);

            body.writeInt(5000); // page_size
            body.writeLong(1743025467097000L); //default timestamp

            buf.writeInt(body.readableBytes());
            buf.writeBytes(body);

            return buf;
        }

        private void writeString(ByteBuf buf, String value, EntityBuilder.TypeTag tag)
        {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            tag.writeLock(buf, bytes.length);
            buf.writeBytes(bytes);

            //buf.nioBuffer().position(bytes.length).asCharBuffer().put(value);
        }

        private void writeLongValue(ByteBuf buf, long value)
        {
            buf.writeInt(8);
            buf.writeLong(value);
        }
    }
}
