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
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjectCreateActionTest extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>, CacheObjectAction<ByteBuf>
{
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();

    protected final List<ObjectCreateAction> cache = new ArrayList<>();

    protected final int bitfield;
    protected final short consistency;

    public ObjectCreateActionTest(LibraryImpl api, byte flags, short consistency, ObjectFlags... objectFlags)
    {
        super(api, flags, SocketCode.QUERY);
        this.consistency = consistency;
        this.bitfield = Arrays.stream(objectFlags).mapToInt(ObjectFlags::getValue).reduce(0, ((result, original) -> result | original));
    }

    public ObjectCreateActionTest(LibraryImpl api, byte flags, Consistency consistency, ObjectFlags... objectFlags)
    {
        this(api, flags, consistency.getCode(), objectFlags);
    }

    @Override
    protected void handleSuccess(@Nonnull Request<ByteBuf> request, @Nonnull Response response)
    {
        request.onSuccess(response.getBody());
    }

    @Nonnull
    @Override
    public ByteBuf asByteBuf()
    {
        byte[] content = this.getContent().getBytes(StandardCharsets.UTF_8);
        int length = content.length;
        int bodyLength = LENGTH + length + Short.BYTES + Byte.BYTES;

        return Unpooled.directBuffer()
                .writeByte(version)
                .writeByte(flags)
                .writeShort(0x00)
                .writeByte(opcode)
                .writeInt(bodyLength)
                .writeInt(length)
                .writeBytes(content)
                .writeShort(consistency)
                .writeByte(bitfield);
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder getBuilder()
    {
        return this.builder;
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

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof ObjectCreateAction))
            return false;

        ObjectCreateAction other = (ObjectCreateAction) obj;
        return other.asByteBuf().equals(this.asByteBuf());
    }
}
