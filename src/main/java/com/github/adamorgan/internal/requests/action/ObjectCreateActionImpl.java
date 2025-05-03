package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.action.CacheObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.request.ObjectCreateBuilder;
import com.github.adamorgan.internal.utils.request.ObjectCreateBuilderMixin;
import com.github.adamorgan.internal.utils.request.ObjectCreateData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

public class ObjectCreateActionImpl extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>, CacheObjectAction<ByteBuf>
{
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();
    protected final Consistency consistency;
    protected boolean useCache = true;
    protected long nonce;

    public ObjectCreateActionImpl(Library api, @Nullable Consistency consistency)
    {
        super((LibraryImpl) api, SocketCode.QUERY);
        this.consistency = consistency == null ? Consistency.ONE : consistency;
    }

    public ObjectCreateActionImpl(Library api)
    {
        this(api, Consistency.ONE);
    }

    @Override
    protected void handleSuccess(@Nonnull Request<ByteBuf> request, @Nonnull Response response)
    {
        int kind = response.getBody().readInt();

        switch (kind)
        {
            case 2:
            {
                request.onSuccess(response.getBody());
                break;
            }
            case 4:
            {
                new ExecuteActionImpl(this, response).queue(request::onSuccess, request::onFailure);
                break;
            }
            default:
            {
                throw new UnsupportedOperationException("Unsupported kind: " + kind);
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
        return null;
    }

    @Nonnull
    @Override
    public Consistency getConsistency()
    {
        return consistency;
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
        Checks.notNegative(timestamp, "Nonce");
        this.nonce = timestamp;
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
    public <R> ObjectCreateAction setContent(@Nullable String content, @Nonnull Collection<? super R> args)
    {
        getBuilder().setContent(content, args);
        return this;
    }

    @Nonnull
    @Override
    public <R> ObjectCreateAction setContent(@Nullable String content, @Nonnull Map<String, ? super R> args)
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
    public ByteBuf asByteBuf()
    {
        return new ObjectCreateData(this).applyData();
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
        return ByteBufUtil.equals(other.asByteBuf(), this.asByteBuf());
    }

    @Override
    public int hashCode()
    {
        return getContent().hashCode();
    }
}
