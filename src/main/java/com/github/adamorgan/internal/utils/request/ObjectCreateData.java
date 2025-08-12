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

import com.github.adamorgan.api.utils.request.ObjectData;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.requests.action.ObjectCreateActionImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

public class ObjectCreateData implements ObjectData
{
    public static final int CONTENT_BYTES = Integer.BYTES;

    private final ObjectCreateActionImpl action;
    private final byte version, opcode;
    private final int flags;
    private final byte[] content;
    private final Compression compression;
    private final ObjectCreateAction.Consistency consistency;
    private final int fields;
    private final int maxBufferSize;
    private final long timestamp;
    private final ByteBuf header;
    private final ByteBuf body, rawBody;

    public ObjectCreateData(ObjectCreateAction action)
    {
        this.action = (ObjectCreateActionImpl) action;
        this.version = this.action.version;
        this.compression = action.getCompression();
        this.flags = action.getRawFlags();
        this.opcode = action.isEmpty() ? SocketCode.QUERY : SocketCode.PREPARE;
        this.content = StringUtils.getBytes(action.getContent(), StandardCharsets.UTF_8);
        this.consistency = action.getConsistency();
        this.fields = action.getFieldsRaw();
        this.maxBufferSize = action.getMaxBufferSize();
        this.timestamp = action.getTimestamp();

        this.rawBody = applyBody();
        this.body = action.getCompression().pack(rawBody);
        this.header = applyHeader();
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return ObjectCreateAction.Field.fromBitFields(fields);
    }

    public ByteBuf applyHeader()
    {
        return Unpooled.directBuffer(finalizeLength() + ObjectAction.HEADER_BYTES)
                .writeByte(version)
                .writeByte(flags)
                .writeShort(0)
                .writeByte(opcode)
                .writeInt(body.readableBytes());
    }

    public ByteBuf applyBody()
    {
        return Unpooled.directBuffer()
                .writeInt(content.length)
                .writeBytes(content)
                .writeShort(consistency.getCode())
                .writeByte(fields)
                .writeInt(maxBufferSize)
                .writeLong(timestamp);
    }

    private int finalizeLength()
    {
        return CONTENT_BYTES + body.readableBytes() + (opcode == SocketCode.QUERY ? Short.BYTES : 0) + ObjectCreateAction.Field.BYTES + ObjectCreateAction.Field.getCapacity(fields);
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        return Unpooled.compositeBuffer().addComponents(true, header, body);
    }

    class Builder
    {
        private int id;

        public Builder(int id, Compression compression, int fields, int bufferSize, long timestamp)
        {

        }

        public Builder setId(int id)
        {
            this.id = id;
            return this;
        }

        public ObjectCacheData build()
        {
            return new ObjectCacheData(action);
        }
    }
}
