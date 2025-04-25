package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.request.ObjectRequest;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractObjectBuilder<T extends AbstractObjectBuilder<T>> implements ObjectRequest<T>
{
    protected final StringBuilder content = new StringBuilder();
    protected int fields = 0;
    protected int maxBufferSize = 5000;
    protected long nonce;
    protected final List<ByteBuf> values = new LinkedList<>();

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

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Fields> getFields()
    {
        return ObjectCreateAction.Fields.fromBitField(this.fields);
    }

    @Nonnull
    @Override
    public List<ByteBuf> getValues()
    {
        return this.values;
    }

    @Nonnull
    public abstract ObjectCreateData build();
}
