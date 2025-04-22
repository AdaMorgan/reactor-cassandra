package com.datastax.test;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class ObjectCreateActionImpl extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>
{
    protected final String content;
    protected final short consistency;
    protected final int bitfield;
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();

    public ObjectCreateActionImpl(LibraryImpl api, byte flags, byte opcode, String content, short consistency, ObjectFlags... queryFlags)
    {
        super(api, flags, opcode);
        this.content = content;
        this.consistency = consistency;
        this.bitfield = Arrays.stream(queryFlags).mapToInt(ObjectFlags::getValue).reduce(0, ((result, original) -> result | original));
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
        byte[] content = this.content.getBytes(StandardCharsets.UTF_8);
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

    protected static ByteBuf rowsResult(LibraryImpl api, ByteBuf buffer)
    {
        int kind = buffer.readInt();

        switch (kind)
        {
            case 0x0001:
                System.out.println("Void: for results carrying no information.");
                break;
            case 0x0002:
                System.out.println("Rows: for results to select queries, returning a set of rows.");
                break;
            case 0x0003:
                System.out.println("Set_keyspace: the result to a `use` query.");
                break;
            case 0x0004:
                System.out.println("Prepared: result to a PREPARE message.");
                break;
            case 0x0005:
                System.out.println("Schema_change: the result to a schema altering query.");
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return null;
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder getBuilder()
    {
        return builder;
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
}
