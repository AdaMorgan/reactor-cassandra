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

package com.github.adamorgan.internal.requests.action;

import com.github.adamorgan.api.Library;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.action.CacheObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.request.ObjectCacheData;
import com.github.adamorgan.internal.utils.request.ObjectCreateData;
import com.github.adamorgan.api.utils.request.ObjectData;
import com.github.adamorgan.internal.utils.request.*;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public class ObjectCreateActionImpl extends ObjectActionImpl<Response> implements ObjectCreateAction, ObjectCreateBuilderMixin<ObjectCreateAction>, CacheObjectAction<Response>
{
    protected final ObjectCreateBuilder builder = new ObjectCreateBuilder();
    protected boolean useCache = true;
    protected boolean useTrace = false;
    protected long timestamp;

    public ObjectCreateActionImpl(Library api)
    {
        super((LibraryImpl) api);
    }

    @Override
    protected void handleSuccess(@Nonnull Request<Response> request, @Nonnull Response response)
    {
        switch (response.getType())
        {
            case ERROR:
            case VOID:
            case ROWS:
            case SET_KEYSPACE:
            case SCHEMA_CHANGE:
            {
                request.onSuccess(response);
                break;
            }
            case PREPARED:
            {
                new ObjectCallbackActionImpl(this, response).queue(request::onSuccess, request::onFailure);
                break;
            }
        }
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder getBuilder()
    {
        return this.builder;
    }

    @Nonnull
    @Override
    public ObjectAction<Response> useTrace(boolean enable)
    {
        this.useTrace = enable;
        return this;
    }

    @Override
    public int getFieldsRaw()
    {
        return this.getBuilder().getFieldsRaw();
    }

    @Nonnull
    @Override
    public Compression getCompression()
    {
        return this.api.getCompression();
    }

    @Override
    public int getRawFlags()
    {
        return (this.getCompression().equals(Compression.NONE) ? 0 : 0x01) | (this.useTrace ? 0x02 : 0);
    }

    @Nonnull
    @Override
    public EnumSet<Flags> getFlags()
    {
        return Flags.fromBitField(getRawFlags());
    }

    @Nonnull
    @Override
    public EnumSet<Field> getFields()
    {
        return this.getBuilder().getFields();
    }

    @Nonnull
    @Override
    public ByteBuf getBody()
    {
        return this.getBuilder().getBody();
    }

    @Nonnull
    @Override
    public Consistency getConsistency()
    {
        return this.getBuilder().getConsistency();
    }

    @Override
    public long getTimestamp()
    {
        return timestamp;
    }

    @Nonnull
    @Override
    public ObjectCreateAction setTimestamp(long timestamp)
    {
        getBuilder().setTimestamp(timestamp);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateAction setConsistency(Consistency consistency)
    {
        getBuilder().setConsistency(consistency);
        return this;
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
    public ObjectCreateAction setContent(@Nonnull String content, @Nonnull ByteBuf body, int size, boolean named)
    {
        getBuilder().setContent(content, body, size, named);
        return this;
    }

    @Nonnull
    @Override
    public CacheObjectAction<Response> useCache(boolean useCache)
    {
        this.useCache = useCache;
        return this;
    }

    @Nonnull
    @Override
    public ObjectData finalizeData()
    {
        return this.api.getObjectCache().containsKey(hashCode()) ? new ObjectCacheData(this) : new ObjectCreateData(this);
    }

    @Override
    public int getMaxBufferSize()
    {
        return this.builder.getMaxBufferSize() != 5000 ? this.builder.getMaxBufferSize() : this.api.getMaxBufferSize();
    }

    @Override
    public boolean isEmpty()
    {
        return this.builder.isEmpty();
    }
}
