package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.request.ObjectRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractObjectBuilder<T extends AbstractObjectBuilder<T>> implements ObjectRequest<T>
{
    protected final StringBuilder content = new StringBuilder();
    protected int fields = ObjectCreateAction.Field.DEFAULT;
    protected int maxBufferSize = 5000;
    protected long nonce;

    protected final ByteBuf body = Unpooled.directBuffer();

    @Nonnull
    @Override
    public ByteBuf getBody()
    {
        return body;
    }

    @Nonnull
    @Override
    public String getContent()
    {
        return content.toString();
    }

    @Override
    public int getFieldsRaw()
    {
        return this.fields;
    }

    @Override
    public int getMaxBufferSize()
    {
        return this.maxBufferSize;
    }

    @Override
    public boolean isEmpty()
    {
        return !this.body.isReadable();
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return ObjectCreateAction.Field.fromBitFields(this.fields);
    }
}
