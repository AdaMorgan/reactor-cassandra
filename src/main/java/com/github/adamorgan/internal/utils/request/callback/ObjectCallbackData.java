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

package com.github.adamorgan.internal.utils.request.callback;

import com.github.adamorgan.annotations.ReplaceWith;
import com.github.adamorgan.api.LibraryInfo;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.MiscUtil;
import com.github.adamorgan.api.utils.request.ObjectData;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.Checks;
import io.netty.buffer.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ObjectCallbackData implements ObjectData
{
    public static final int CODE = SocketCode.PREPARE;

    private final int flags;
    private final byte[] content;
    private final List<? extends ByteBuf> args;
    private final ByteBuf argsBody;
    private final Compression compression;
    private final int largeThreshold;
    private final int fields;
    private final int maxBufferSize;
    private final long timestamp;

    private final ByteBuf body;
    private final int bucketId;

    private ObjectCallbackData(int flags, byte[] content, List<? extends ByteBuf> args, Compression compression, int largeThreshold, int fields, int bufferSize, long timestamp)
    {
        this.flags = flags;
        this.content = content;
        this.args = args;
        this.compression = compression;
        this.largeThreshold = largeThreshold;
        this.fields = fields;
        this.maxBufferSize = bufferSize;
        this.timestamp = timestamp;

        this.argsBody = args.stream().collect(Unpooled::directBuffer, ByteBuf::writeBytes, ByteBuf::writeBytes);

        ByteBuf rawBody = applyBody();
        this.body = compression.pack(rawBody);
        this.bucketId = MiscUtil.getBucketId(body);
    }

    @Nonnull
    private ByteBuf applyBody()
    {
        return Unpooled.directBuffer()
                .writeInt(content.length)
                .writeBytes(content)
                .writeShort(largeThreshold)
                .writeByte(fields)
                .writeInt(maxBufferSize)
                .writeLong(timestamp);
    }

    @Nonnull
    public static ObjectCallbackData.Builder create(@Nonnull String content, List<? extends ByteBuf> args, int flags)
    {
        Checks.notBlank(content, "Content");
        Checks.notNegative(flags, "Flags");
        return new ObjectCallbackData.Builder(content, args, flags);
    }

    @Override
    public ByteBuf asByteBuf()
    {
        return Unpooled.directBuffer()
                .writeByte(LibraryInfo.PROTOCOL_VERSION)
                .writeByte(flags)
                .writeShort(bucketId)
                .writeByte(CODE)
                .writeInt(body.readableBytes())
                .writeBytes(body);
    }

    @Override
    public int hashCode()
    {
        return bucketId;
    }

    public static class Builder
    {
        protected final byte[] content;
        protected final List<? extends ByteBuf> args;
        protected final int flags;

        protected Compression compression = Compression.NONE;
        protected int largeThreshold = 1;
        protected int fields = 0;
        protected int maxBufferSize = 2048;
        protected long timestamp = System.currentTimeMillis();

        protected Builder(@Nonnull String content, List<? extends ByteBuf> args, int flags)
        {
            this.content = StringUtils.getBytes(content, StandardCharsets.UTF_8);
            this.args = args;
            this.flags = flags;
        }

        @Nonnull
        public Builder setCompression(@Nonnull Compression compression)
        {
            Checks.notNull(compression, "Compression");
            this.compression = compression;
            return this;
        }

        @Nonnull
        public Builder setLargeThreshold(int threshold)
        {
            Checks.inRange(threshold, 0, 10, "Threshold");
            this.largeThreshold = threshold;
            return this;
        }

        @Nonnull
        @ReplaceWith
        public Builder setFields(int fields)
        {
            Checks.notNegative(fields, "Fields");
            this.fields = fields;
            return this;
        }

        @Nonnull
        public Builder setMaxBufferSize(int bufferSize)
        {
            Checks.notNegative(bufferSize, "The buffer size");
            this.maxBufferSize = bufferSize;
            return this;
        }

        @Nonnull
        public Builder setTimestamp(long timestamp)
        {
            Checks.notNegative(timestamp, "The Time");
            this.timestamp = timestamp;
            return this;
        }

        @Nonnull
        public ObjectCallbackData build()
        {
            return new ObjectCallbackData(flags, content, args, compression, largeThreshold, fields, maxBufferSize, timestamp);
        }
    }
}
