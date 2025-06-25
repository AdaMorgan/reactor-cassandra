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

package com.github.adamorgan.internal.utils.compress;

import com.github.adamorgan.api.utils.Compression;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public class NoopCompressor<T extends ByteBuf> implements Compressor<T>
{
    @Nonnull
    @Override
    public Compression getType()
    {
        return Compression.NONE;
    }

    @Nonnull
    @Override
    public T pack(T body)
    {
        return body;
    }

    @Nonnull
    @Override
    public T unpack(T body)
    {
        return body;
    }

    @Nonnull
    @Override
    public T packWithoutLength(T body)
    {
        return body;
    }

    @Nonnull
    @Override
    public T unpackWithoutLength(T body, int length)
    {
        return body;
    }
}
