package com.datastax.test;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.requests.action.CacheObjectAction;
import com.datastax.api.requests.objectaction.ObjectCreateAction;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.internal.utils.request.ObjectCreateBuilder;
import com.datastax.internal.utils.request.ObjectCreateBuilderMixin;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjectCreateActionImpl extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>, CacheObjectAction<ByteBuf>
{
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();

    protected final List<ObjectCreateAction> cache = new ArrayList<>();
    protected final Consistency consistency;

    public ObjectCreateActionImpl(LibraryImpl api, byte flags, @Nullable Consistency consistency)
    {
        super(api, flags, SocketCode.QUERY);
        this.consistency = consistency == null ? Consistency.ONE : consistency;
    }

    public ObjectCreateActionImpl(LibraryImpl api, byte flags)
    {
        this(api, flags, null);
    }

    @Override
    protected void handleSuccess(@Nonnull Request<ByteBuf> request, @Nonnull Response response)
    {
        ByteBuf body = response.getBody();
        int kind = body.readInt();
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
    public byte getFieldsRaw()
    {
        return this.getBuilder().getFieldsRaw();
    }

    @Override
    public EnumSet<Fields> getFields()
    {
        return this.getBuilder().getFields();
    }

    @Nonnull
    @Override
    public Consistency getConsistency()
    {
        return consistency;
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
        return this;
    }

    @Nonnull
    @Override
    public ByteBuf asByteBuf()
    {
        byte[] content = this.getContent().getBytes(StandardCharsets.UTF_8);

        int length = content.length;
        int bodyLength = LENGTH + length + Short.BYTES + Byte.BYTES;

        byte opcode = this.builder.getValues().isEmpty() ? SocketCode.QUERY : SocketCode.PREPARE;

        return Unpooled.directBuffer()
                .writeByte(version)
                .writeByte(flags)
                .writeShort(0x00)
                .writeByte(opcode)
                .writeInt(bodyLength)
                .writeInt(length)
                .writeBytes(content)
                .writeShort(consistency.getCode())
                .writeByte(getFieldsRaw());
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
}
