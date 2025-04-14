package com.datastax.test.action.session;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketClient;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class StartingActionImpl extends ObjectActionImpl<ByteBuf>
{
    private static final String CQL_VERSION_OPTION = "CQL_VERSION";
    private static final String CQL_VERSION = "3.0.0";

    private static final String DRIVER_VERSION_OPTION = "DRIVER_VERSION";
    private static final String DRIVER_VERSION = "0.2.0";

    private static final String DRIVER_NAME_OPTION = "DRIVER_NAME";
    private static final String DRIVER_NAME = "mmorrii one love!";

    private static final String THROW_ON_OVERLOAD_OPTION = "THROW_ON_OVERLOAD";
    private static final String THROW_ON_OVERLOAD = "true";

    static final String COMPRESSION_OPTION = "COMPRESSION";
    static final String NO_COMPACT_OPTION = "NO_COMPACT";

    public StartingActionImpl(LibraryImpl api, byte flags)
    {
        super(api, flags, SocketCode.STARTUP);
    }

    @Override
    protected void handleSuccess(@Nonnull Request<ByteBuf> request, Response response)
    {
        request.onSuccess(response.getBody());
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        Map<String, String> map = new HashMap<>();
        map.put(CQL_VERSION_OPTION, CQL_VERSION);
        map.put(DRIVER_VERSION_OPTION, DRIVER_VERSION);
        map.put(DRIVER_NAME_OPTION, DRIVER_NAME);
        map.put(THROW_ON_OVERLOAD_OPTION, THROW_ON_OVERLOAD);

        ByteBuf body = Unpooled.directBuffer();

        body.writeShort(map.size());

        for (Map.Entry<String, String> entry : map.entrySet())
        {
            writeString(body, entry.getKey());
            writeString(body, entry.getValue());
        }

        return new EntityBuilder()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode)
                .writeBytes(body)
                .asByteBuf();
    }

    public void writeString(ByteBuf body, String value)
    {
        byte[] bytes = value.getBytes(CharsetUtil.UTF_8);
        body.writeShort(bytes.length);
        body.writeBytes(bytes);
    }
}
