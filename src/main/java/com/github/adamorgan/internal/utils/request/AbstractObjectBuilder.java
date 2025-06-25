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

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.request.ObjectRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public abstract class AbstractObjectBuilder<T extends AbstractObjectBuilder<T>> implements ObjectRequest<T>
{
    protected final StringBuilder content = new StringBuilder();
    protected int fields = ObjectCreateAction.Field.DEFAULT;
    protected ObjectCreateAction.Consistency consistency = ObjectCreateAction.Consistency.ONE;
    protected Compression compression = Compression.NONE;
    protected int maxBufferSize = 5000;
    protected long nonce;

    protected final ByteBuf body = UnpooledByteBufAllocator.DEFAULT.directBuffer(maxBufferSize);

    @Nonnull
    @Override
    public ByteBuf getBody()
    {
        return body;
    }

    @Nonnull
    @Override
    public String getContent()
    {
        return content.toString();
    }

    @Override
    public int getFieldsRaw()
    {
        return this.fields;
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return ObjectCreateAction.Field.fromBitFields(this.fields);
    }

    @Override
    public int getMaxBufferSize()
    {
        return this.maxBufferSize;
    }

    @Override
    public boolean isEmpty()
    {
        return !this.body.isReadable();
    }

    @Nonnull
    @Override
    public Consistency getConsistency()
    {
        return this.consistency;
    }

    @Override
    public long getNonce()
    {
        return nonce;
    }
}
