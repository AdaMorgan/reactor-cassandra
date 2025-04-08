package com.datastax.test.action;

import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.api.requests.objectaction.ObjectCreateAction;
import com.datastax.api.utils.request.ObjectCreateRequest;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import com.datastax.internal.requests.action.ObjectActionImpl;
import com.datastax.internal.utils.request.ObjectCreateBuilder;
import com.datastax.internal.utils.request.ObjectCreateBuilderMixin;
import com.datastax.test.EntityBuilder;
import com.datastax.test.RowsResultImpl;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ObjectCreateActionImpl extends ObjectActionImpl<ByteBuf> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>
{
    protected final String content;
    protected final int level;
    protected final int bitfield;
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();

    protected ObjectCreateActionImpl(LibraryImpl api, byte version, byte flags, byte opcode, String content, Level level, Flag... queryFlags)
    {
        super(api, version, flags, opcode);
        this.content = content;
        this.level = level.getCode();
        this.bitfield = Arrays.stream(queryFlags).mapToInt(Flag::getValue).reduce(0, ((result, original) -> result | original));
    }

    public ObjectCreateActionImpl(LibraryImpl api, byte version, byte flags, String content, Level level, Flag... queryFlags)
    {
        this(api, version, flags, SocketCode.QUERY, content, level, queryFlags);
    }

    @Override
    protected void handleSuccess(Request<ByteBuf> request, Response response)
    {
        request.onSuccess(response.getBody());
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        byte[] queryBytes = content.getBytes(StandardCharsets.UTF_8);

        int messageLength = 4 + queryBytes.length + 2 + 1;

        return new EntityBuilder(1 + 4 + messageLength)
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(0x00)
                .writeByte(this.opcode)
                .writeInt(messageLength)
                .writeString(content)
                .writeShort(this.level)
                .writeByte(this.bitfield)
                .asByteBuf();
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
                new RowsResultImpl(buffer).run();
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
    public ObjectCreateAction addContent(@Nonnull String content)
    {
        return null;
    }
}
