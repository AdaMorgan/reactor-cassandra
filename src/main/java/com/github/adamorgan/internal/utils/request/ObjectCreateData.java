/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.internal.utils.request;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

public class ObjectCreateData implements ObjectData
{
    public static final int CONTENT_BYTES = Integer.BYTES;

    private final byte version, opcode;
    private final int flags;
    private final int stream;
    private final byte[] content;
    private final Compression compression;
    private final ObjectCreateAction.Consistency consistency;
    private final int fields;
    private final int maxBufferSize;
    private final long nonce;
    private final ByteBuf header;
    private final ByteBuf body;

    public ObjectCreateData(ObjectCreateAction action, byte version, int stream)
    {
        this.version = version;
        this.compression = action.getCompression();
        this.flags = action.getRawFlags();
        this.stream = stream;
        this.opcode = action.isEmpty() ? SocketCode.QUERY : SocketCode.PREPARE;
        this.content = StringUtils.getBytes(action.getContent(), StandardCharsets.UTF_8);
        this.consistency = action.getConsistency();
        this.fields = action.getFieldsRaw();
        this.maxBufferSize = action.getMaxBufferSize();
        this.nonce = action.getNonce();
        this.body = this.compression.pack(applyBody());
        this.header = applyHeader();
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return ObjectCreateAction.Field.fromBitFields(fields);
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        return Unpooled.compositeBuffer(2).addComponents(true, this.header, this.body);
    }

    public ByteBuf applyHeader()
    {
        return Unpooled.directBuffer(finalizeLength() + ObjectAction.HEADER_BYTES)
                .writeByte(version)
                .writeByte(flags)
                .writeShort(stream)
                .writeByte(opcode)
                .writeInt(body.readableBytes())
                .asByteBuf();
    }

    public ByteBuf applyBody()
    {
        return Unpooled.directBuffer()
                .writeInt(content.length)
                .writeBytes(content)
                .writeShort(consistency.getCode())
                .writeByte(fields)
                .writeInt(maxBufferSize)
                .writeLong(nonce)
                .asByteBuf();
    }

    private int finalizeLength()
    {
        return CONTENT_BYTES + body.readableBytes() + (opcode == SocketCode.QUERY ? Short.BYTES : 0) + ObjectCreateAction.Field.BYTES + ObjectCreateAction.Field.getCapacity(fields);
    }
}
