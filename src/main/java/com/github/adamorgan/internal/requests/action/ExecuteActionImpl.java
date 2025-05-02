package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.utils.data.DataType;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.UnlockHook;
import com.github.adamorgan.internal.utils.cache.ObjectCacheViewImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;

public final class ExecuteActionImpl extends ObjectActionImpl<ByteBuf>
{
    private final ObjectCreateActionImpl action;
    private final ByteBuf response;
    private final int consistency, fields;

    public ExecuteActionImpl(ObjectCreateActionImpl action, Response response)
    {
        super((LibraryImpl) action.getLibrary(), SocketCode.EXECUTE);
        this.action = action;
        this.response = response.getBody();
        this.consistency = action.getConsistency().getCode();
        this.fields = action.getFieldsRaw();
    }

    @Override
    protected void handleSuccess(Request<ByteBuf> request, Response response)
    {
        this.action.handleSuccess(request, response);
    }

    public ByteBuf execute()
    {
        int idLength = this.response.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        this.response.readBytes(preparedId);

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(this.consistency);

        body.writeByte(this.fields);

        //--- size
        body.writeShort(2);

        //--- 1
        writeLongValue(body, 123456L);

        //--- 2
        DataType.LONG_STRING.encode(body, "reganjohn");

        //--- flags
        body.writeInt(5000); // page size
        body.writeLong(System.currentTimeMillis()); // timestamp

        buf.writeInt(body.readableBytes());
        buf.writeBytes(body);

        ObjectCacheViewImpl objectCache = this.api.getObjectCache();

        try (UnlockHook hook = objectCache.writeLock())
        {
            objectCache.getMap().put(action.hashCode(), buf.copy());
        }

        return buf;
    }

    @Nonnull
    @Override
    public ByteBuf asByteBuf()
    {
        return execute();
    }

    public ByteBuf executeParameters(ByteBuf buffer)
    {
        int idLength = buffer.readUnsignedShort();
        byte[] preparedId = new byte[idLength];
        buffer.readBytes(preparedId);

        ByteBuf buf = Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.fields)
                .writeShort(0x03)
                .writeByte(this.opcode);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(preparedId.length);
        body.writeBytes(preparedId);

        body.writeShort(this.consistency);

        body.writeByte(this.fields);

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
