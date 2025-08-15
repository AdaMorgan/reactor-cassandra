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
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.action.CacheObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCallbackAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.api.utils.request.ObjectData;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.request.ObjectCallbackBuilderMixin;
import com.github.adamorgan.internal.utils.request.callback.ObjectCacheData;
import com.github.adamorgan.internal.utils.request.callback.ObjectCallbackBuilder;
import com.github.adamorgan.internal.utils.request.callback.ObjectCallbackData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjectCallbackActionImpl extends ObjectActionImpl<Response> implements ObjectCallbackAction, ObjectCallbackBuilderMixin<ObjectCallbackAction>, CacheObjectAction<Response>
{
    protected final ObjectCallbackBuilder builder = new ObjectCallbackBuilder();
    protected boolean useCache = true;
    protected boolean useTrace = false;
    protected long timestamp;

    public ObjectCallbackActionImpl(@Nonnull Library api)
    {
        super((LibraryImpl) api);
    }

    @Override
    protected void handleSuccess(Request<Response> request, Response response)
    {
        if (response.getType().equals(Response.Type.PREPARED))
        {
            this.api.getObjectCache().cache(hashCode(), finalizeData().asByteBuf());

            short length = response.getBody().readShort();
            byte[] id = new byte[length];
            response.getBody().readBytes(id);
        }
        else
        {
            request.onSuccess(response);
        }
    }

    @Nonnull
    @Override
    public ObjectCallbackBuilder getBuilder()
    {
        return builder;
    }

    @Nonnull
    @Override
    public ObjectCallbackAction useTrace(boolean enabled)
    {
        getBuilder().useTrace(enabled);
        return this;
    }

    @Override
    public boolean isTrace()
    {
        return getBuilder().isTrace();
    }

    @Nullable
    @Override
    public ByteBuf getToken()
    {
        return this.api.getObjectCache().get(hashCode());
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
    public ObjectCreateAction.Consistency getConsistency()
    {
        return getBuilder().getConsistency();
    }

    @Override
    public long getTimestamp()
    {
        return getBuilder().getTimestamp();
    }

    @Nonnull
    @Override
    public List<? extends ByteBuf> getAttachments()
    {
        return getBuilder().getAttachments();
    }

    @Nonnull
    @Override
    public ObjectCallbackAction setContent(@Nullable String content, @Nonnull Collection<? super Serializable> args)
    {
        Checks.notNull(content, "args");
        getBuilder().setContent(content, args);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCallbackAction setContent(@Nullable String content, @Nonnull Map<String, ? super Serializable> args)
    {
        Checks.notNull(args, "args");
        getBuilder().setContent(content, args);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCallbackAction setTimestamp(long timestamp)
    {
        getBuilder().setTimestamp(timestamp);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCallbackAction setConsistency(@Nonnull Consistency consistency)
    {
        getBuilder().setConsistency(consistency);
        return this;
    }

    @Nonnull
    @Override
    public String getContent()
    {
        return getBuilder().getContent();
    }

    @Override
    public int getFieldsRaw()
    {
        return getBuilder().getFieldsRaw();
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return getBuilder().getFields();
    }

    @Override
    public boolean useCache()
    {
        return useCache;
    }

    @Override
    public int getMaxBufferSize()
    {
        return getBuilder().getMaxBufferSize();
    }

    @Nonnull
    @Override
    public ObjectData finalizeData()
    {
        return ObjectCallbackData.create(getContent(), getAttachments(), getRawFlags())
                .setCompression(getCompression())
                .setLargeThreshold(getConsistency().getCode())
                .setFields(getFieldsRaw())
                .setMaxBufferSize(getMaxBufferSize())
                .setTimestamp(getTimestamp())
                .build();
    }

    @Nonnull
    @Override
    public CacheObjectAction<Response> useCache(boolean enabled)
    {
        this.useCache = enabled;
        return this;
    }
}
