package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.action.CacheObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.request.ObjectCreateBuilder;
import com.github.adamorgan.internal.utils.request.ObjectCreateBuilderMixin;
import com.github.adamorgan.internal.utils.request.ObjectCreateData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

public class ObjectCreateActionImpl extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>, CacheObjectAction<ByteBuf>
{
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();
    protected boolean useCache = true;
    protected long nonce;

    public ObjectCreateActionImpl(Library api)
    {
        super((LibraryImpl) api, SocketCode.QUERY);
    }

    @Override
    protected void handleSuccess(@Nonnull Request<ByteBuf> request, @Nonnull Response response)
    {
        switch (response.getType())
        {
            case VOID:
            {
                throw new UnsupportedOperationException();
            }
            case ROWS:
            {
                request.onSuccess(response.getBody());
                break;
            }
            case SET_KEYSPACE:
            {
                throw new UnsupportedOperationException();
            }
            case PREPARED:
            {
                new ObjectCallbackActionImpl(this, response).queue(request::onSuccess, request::onFailure);
                break;
            }
            case SCHEMA_CHANGE:
            {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder getBuilder()
    {
        return this.builder;
    }

    @Override
    public int getFieldsRaw()
    {
        return this.getBuilder().getFieldsRaw();
    }

    @Nonnull
    @Override
    public EnumSet<Field> getFields()
    {
        return this.getBuilder().getFields();
    }

    @Nonnull
    @Override
    public ByteBuf getBody()
    {
        return this.getBuilder().getBody();
    }

    @Nonnull
    @Override
    public Consistency getConsistency()
    {
        return this.getBuilder().getConsistency();
    }

    @Override
    public long getNonce()
    {
        return nonce != 0 ? nonce : System.currentTimeMillis();
    }

    @Nonnull
    @Override
    public ObjectCreateAction setNonce(long timestamp)
    {
        getBuilder().setNonce(timestamp);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateAction setConsistency(Consistency consistency)
    {
        getBuilder().setConsistency(consistency);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateAction addContent(@Nonnull String content)
    {
        getBuilder().addContent(content);
        return this;
    }

    @Nonnull
    @Override
    public <R extends Serializable> ObjectCreateAction setContent(@Nullable String content, @Nonnull Collection<? extends R> args)
    {
        getBuilder().setContent(content, args);
        return this;
    }

    @Nonnull
    @Override
    public <R extends Serializable> ObjectCreateAction setContent(@Nullable String content, @Nonnull Map<String, ? extends R> args)
    {
        getBuilder().setContent(content, args);
        return this;
    }

    @Nonnull
    @Override
    public CacheObjectAction<ByteBuf> useCache(boolean useCache)
    {
        this.useCache = useCache;
        return this;
    }

    @Nonnull
    @Override
    public ByteBuf finalizeData()
    {
        short stream = 0x00;
        return new ObjectCreateData(this, version, flags, stream).applyData();
    }

    @Override
    public int getMaxBufferSize()
    {
        return this.builder.getMaxBufferSize() != 5000 ? this.builder.getMaxBufferSize() : this.api.getMaxBufferSize();
    }

    @Override
    public boolean isEmpty()
    {
        return this.builder.isEmpty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof ObjectCreateAction))
            return false;
        ObjectCreateAction other = (ObjectCreateAction) obj;
        return ByteBufUtil.equals(other.finalizeData(), this.finalizeData());
    }

    @Override
    public int hashCode()
    {
        return getContent().hashCode();
    }
}
