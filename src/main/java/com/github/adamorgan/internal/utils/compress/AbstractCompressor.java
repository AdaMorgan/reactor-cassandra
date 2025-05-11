/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.adamorgan.internal.utils.compress;

import com.github.adamorgan.api.utils.Compression;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

public abstract class AbstractCompressor implements Compressor<ByteBuf>
{
    protected final Compression compression;

    protected AbstractCompressor(Compression compression)
    {
        this.compression = compression;
    }

    @Nonnull
    @Override
    public Compression getType()
    {
        return compression;
    }

    @Nonnull
    @Override
    public ByteBuf pack(ByteBuf body)
    {
        return body.isDirect() ? packDirect(body, true) : packHeap(body, true);
    }

    @Nonnull
    @Override
    public ByteBuf packWithoutLength(ByteBuf body)
    {
        return body.isDirect() ? packDirect(body, false) : packHeap(body, false);
    }

    protected abstract ByteBuf packDirect(ByteBuf body, boolean prependWithUncompressedLength);

    protected abstract ByteBuf packHeap(ByteBuf body, boolean prependWithUncompressedLength);

    @Nonnull
    @Override
    public ByteBuf unpack(ByteBuf body)
    {
        return unpackWithoutLength(body, readUncompressedLength(body));
    }

    protected abstract int readUncompressedLength(ByteBuf body);

    @Nonnull
    @Override
    public ByteBuf unpackWithoutLength(ByteBuf body, int length)
    {
        return body.isDirect() ? unpackDirect(body, length) : unpackHeap(body, length);
    }

    protected abstract ByteBuf unpackDirect(ByteBuf input, int length);

    protected abstract ByteBuf unpackHeap(ByteBuf input, int length);

    protected static ByteBuffer inputNioBuffer(ByteBuf body)
    {
        // Using internalNioBuffer(...) as we only hold the reference in this method and so can
        // reduce Object allocations.
        int index = body.readerIndex();
        int len = body.readableBytes();
        return body.nioBufferCount() == 1 ? body.internalNioBuffer(index, len) : body.nioBuffer(index, len);
    }

    protected static ByteBuffer outputNioBuffer(ByteBuf body)
    {
        int index = body.writerIndex();
        int len = body.writableBytes();
        return body.nioBufferCount() == 1 ? body.internalNioBuffer(index, len) : body.nioBuffer(index, len);
    }
}
