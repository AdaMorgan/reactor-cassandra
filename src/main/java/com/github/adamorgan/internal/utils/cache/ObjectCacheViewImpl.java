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

package com.github.adamorgan.internal.utils.cache;

import com.github.adamorgan.api.utils.cache.ObjectCacheView;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ObjectCacheViewImpl extends AbstractCacheViewImpl<ByteBuf> implements ObjectCacheView
{
    public ObjectCacheViewImpl(Class<ByteBuf> type)
    {
        super(type);
    }

    @Nonnull
    @Override
    public ByteBuf cache(int token, @Nonnull ByteBuf element)
    {
        ByteBuf readOnly = element.retainedDuplicate().asReadOnly();
        super.cache(token, readOnly);
        return element;
    }

    @Nullable
    @Override
    public ByteBuf get(int hashCode)
    {
        ByteBuf buffer = super.get(hashCode);
        if (buffer == null)
            return null;
        return buffer.duplicate().asReadOnly();
    }
}
