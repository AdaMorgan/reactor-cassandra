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

package com.github.adamorgan.api.utils;

import com.github.adamorgan.internal.utils.compress.Compressor;
import com.github.adamorgan.internal.utils.compress.Lz4Compressor;
import com.github.adamorgan.internal.utils.compress.NoopCompressor;
import com.github.adamorgan.internal.utils.compress.SnappyCompressor;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

/**
 * Compression algorithms that can be used with CQL Binary Protocol.
 *
 * @see com.github.adamorgan.api.LibraryBuilder#setCompression(Compression)
 */
public enum Compression
{
    NONE("", new NoopCompressor<>()),
    LZ4("lz4", new Lz4Compressor()),
    SNAPPY("snappy", new SnappyCompressor());

    private final String key;
    private final Compressor<ByteBuf> compressor;

    Compression(String key, Compressor<ByteBuf> compressor)
    {
        this.key = key;
        this.compressor = compressor;
    }

    @Override
    public String toString()
    {
        return key;
    }

    @Nonnull
    public ByteBuf pack(ByteBuf body)
    {
        try
        {
            return compressor.pack(body);
        }
        finally
        {
            body.release();
        }
    }

    @Nonnull
    public ByteBuf unpack(ByteBuf body)
    {
        try
        {
            return compressor.unpack(body);
        }
        finally
        {
            body.release();
        }
    }
}
