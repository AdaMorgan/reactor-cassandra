package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class ObjectCreateData
{
    protected final ByteBuf header;
    protected final String content;
    protected final int maxBufferSize;
    protected final int flags;
    protected final List<ByteBuf> values;
    protected final ObjectCreateAction.Consistency consistency;

    public ObjectCreateData(ByteBuf header, String content, List<ByteBuf> values, ObjectCreateAction.Consistency consistency, int flags, int maxBufferSize)
    {
        this.header = header;
        this.content = content;
        this.values = Collections.unmodifiableList(values);
        this.consistency = consistency;
        this.flags = flags;
        this.maxBufferSize = maxBufferSize;
    }

    public int getMaxBufferSize()
    {
        return maxBufferSize;
    }

    public ByteBuf asByteBuf()
    {
        byte[] queryBytes = content.getBytes(StandardCharsets.UTF_8);
        int messageLength = 4 + queryBytes.length + 2 + 1;

        return Unpooled.directBuffer()
                .writeBytes(header)
                .writeInt(messageLength)
                .writeInt(queryBytes.length)
                .writeBytes(queryBytes)
                .writeShort(consistency.getCode())
                .writeByte(flags)
                .asByteBuf();
    }
}
