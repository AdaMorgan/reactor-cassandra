package com.datastax.test.action;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.requests.action.CacheObjectAction;
import com.datastax.api.requests.objectaction.ObjectCreateAction;
import com.datastax.api.utils.data.DataValue;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.internal.utils.Checks;
import com.datastax.internal.utils.request.ObjectCreateBuilder;
import com.datastax.internal.utils.request.ObjectCreateBuilderMixin;
import com.datastax.test.EntityBuilder;
import io.netty.buffer.ByteBuf;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjectCreateActionTest extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>, CacheObjectAction<ByteBuf>
{
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();
    protected final LinkedList<ByteBuf> values = new LinkedList<>();

    protected final List<ObjectCreateAction> cache = new ArrayList<>();

    protected final int bitfield;
    protected final short level;

    public ObjectCreateActionTest(LibraryImpl api, byte flags, short level, ObjectFlags... objectFlags)
    {
        super(api, flags, SocketCode.QUERY);

        this.level = level;
        this.bitfield = Arrays.stream(objectFlags).mapToInt(ObjectFlags::getValue).reduce(0, ((result, original) -> result | original));
    }

    @Override
    protected void handleSuccess(@Nonnull Request<ByteBuf> request, @Nonnull Response response)
    {
        request.onSuccess(response.getBody());
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        byte version = this.version;
        byte flags = this.flags;
        short stream = 0x00;
        byte opcode = this.values.isEmpty() ? this.opcode : SocketCode.PREPARE;

        String content = this.getContent();

        byte[] queryBytes = content.getBytes(StandardCharsets.UTF_8);

        int messageLength = 4 + queryBytes.length + 2 + 1;

        return new EntityBuilder(1 + 4 + messageLength).writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode)
                .writeInt(messageLength)
                .writeString(content)
                .writeShort(this.level)
                .writeByte(this.bitfield)
                .asByteBuf();
    }

    @Nonnull
    @Override
    public ObjectCreateAction addContent(@Nonnull String content)
    {
        getBuilder().addContent(content);
        return this;
    }

    @Nonnull
    public <R> ObjectCreateAction addValue(@Nullable R... values)
    {
        return addValue(values == null ? Collections.emptyList() : Arrays.asList(values));
    }

    @Nonnull
    public <R> ObjectCreateAction addValue(@Nonnull Collection<? super R> values)
    {
        values.stream().map(DataValue::new).map(DataValue::asByteBuf).forEachOrdered(this.values::add);
        return this;
    }

    @Nonnull
    public <R> ObjectCreateAction addValue(Map<String, ? super R> values)
    {
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
    public ObjectCreateBuilder getBuilder()
    {
        return this.builder;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof ObjectCreateAction))
            return false;

        ObjectCreateAction other = (ObjectCreateAction) obj;
        return super.equals(obj);
    }
}
