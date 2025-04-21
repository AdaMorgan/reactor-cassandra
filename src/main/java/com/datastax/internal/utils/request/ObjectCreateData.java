package com.datastax.internal.utils.request;

import com.datastax.api.requests.objectaction.ObjectCreateAction;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

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

        return new EntityBuilder()
                .put(header)
                .put(messageLength)
                .put(content)
                .put(consistency.getCode())
                .put(flags)
                .asByteBuf();
    }
}
