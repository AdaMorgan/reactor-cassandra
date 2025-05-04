package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.objectaction.ObjectCallbackAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.request.ObjectCallbackData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public final class ObjectCallbackActionImpl extends ObjectActionImpl<ByteBuf> implements ObjectCallbackAction
{
    private final ByteBuf token;
    private final ObjectCreateActionImpl action;
    private final ByteBuf response;
    private final int length;

    public ObjectCallbackActionImpl(@Nonnull ObjectCreateActionImpl action, @Nonnull Response response)
    {
        super((LibraryImpl) action.getLibrary(), SocketCode.EXECUTE);
        this.action = action;
        this.response = response.getBody();
        this.length = this.response.readUnsignedShort();
        this.token = this.response.readSlice(length);
    }

    @Override
    protected void handleSuccess(Request<ByteBuf> request, Response response)
    {
        this.action.handleSuccess(request, response);
    }

    @Nonnull
    @Override
    public ByteBuf getToken()
    {
        return this.token;
    }

    @Override
    public byte getFlagsRaw()
    {
        return this.action.getFlagsRaw();
    }

    @Nonnull
    @Override
    public EnumSet<Flags> getFlags()
    {
        return this.action.getFlags();
    }

    @Nonnull
    @Override
    public ObjectCreateAction.Consistency getConsistency()
    {
        return this.action.getConsistency();
    }

    @Override
    public long getNonce()
    {
        return this.action.getNonce();
    }

    @Nonnull
    @Override
    public String getContent()
    {
        return this.action.getContent();
    }

    @Override
    public int getFieldsRaw()
    {
        return this.action.getFieldsRaw();
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return this.action.getFields();
    }

    @Nonnull
    @Override
    public ByteBuf getBody()
    {
        return this.action.getBody();
    }

    @Override
    public int getMaxBufferSize()
    {
        return this.action.getMaxBufferSize();
    }

    @Nonnull
    @Override
    public ByteBuf finalizeData()
    {
        short stream = (short) this.stream;
        return new ObjectCallbackData(this, version, flags, stream).applyData();
    }

    @Override
    public boolean isEmpty()
    {
        return this.action.isEmpty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof ObjectCallbackAction))
            return false;
        ObjectCallbackAction other = (ObjectCallbackAction) obj;
        return ByteBufUtil.equals(other.finalizeData(), this.finalizeData());
    }
}
