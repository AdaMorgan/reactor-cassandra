package com.datastax.test.action.session;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.ObjectActionImpl;
import com.datastax.internal.requests.SocketClientRelese;
import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class StartingActionImpl extends ObjectActionImpl<SocketClientRelese.StartingNode>
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

    public StartingActionImpl(LibraryImpl api, int version, int flags, short stream, BiFunction<Request<SocketClientRelese.StartingNode>, Response, SocketClientRelese.StartingNode> handler)
    {
        super(api, version, flags, stream, SocketCode.STARTUP, handler);
    }

    @Override
    public ByteBuf finalizeBuffer()
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

        return body;
    }

    public void writeString(ByteBuf body, String value)
    {
        byte[] bytes = value.getBytes(CharsetUtil.UTF_8);
        body.writeShort(bytes.length);
        body.writeBytes(bytes);
    }
}
