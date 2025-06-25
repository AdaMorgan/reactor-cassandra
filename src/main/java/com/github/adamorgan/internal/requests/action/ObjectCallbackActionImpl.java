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

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.objectaction.ObjectCallbackAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.request.ObjectCallbackData;
import com.github.adamorgan.internal.utils.request.ObjectData;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Objects;

public final class ObjectCallbackActionImpl extends ObjectActionImpl<Response> implements ObjectCallbackAction
{
    private final ByteBuf token;
    private final ObjectCreateActionImpl action;
    private final ByteBuf response;
    private final int length;

    public ObjectCallbackActionImpl(@Nonnull ObjectCreateActionImpl action, @Nonnull Response response)
    {
        super((LibraryImpl) action.getLibrary());
        this.action = action;
        this.response = response.getBody();
        this.length = this.response.readUnsignedShort();
        this.token = this.response.readSlice(length);
    }

    @Override
    protected void handleSuccess(Request<Response> request, Response response)
    {
        this.action.handleSuccess(request, response);
    }

    @Nonnull
    @Override
    public ObjectAction<Response> useTrace(boolean enable)
    {
        return this.action.useTrace(enable);
    }

    @Nonnull
    @Override
    public ByteBuf getToken()
    {
        return this.token;
    }

    @Nonnull
    @Override
    public Compression getCompression()
    {
        return this.action.getCompression();
    }

    @Override
    public int getRawFlags()
    {
        return this.action.getRawFlags();
    }

    @Nonnull
    @Override
    public EnumSet<Flags> getFlags()
    {
        return this.action.getFlags();
    }

    @Nonnull
    @Override
    public ObjectCreateAction.Consistency getConsistency()
    {
        return this.action.getConsistency();
    }

    @Override
    public long getNonce()
    {
        return this.action.getNonce();
    }

    @Nonnull
    @Override
    public String getContent()
    {
        return this.action.getContent();
    }

    @Override
    public int getFieldsRaw()
    {
        return this.action.getFieldsRaw();
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return this.action.getFields();
    }

    @Nonnull
    @Override
    public ByteBuf getBody()
    {
        return this.action.getBody();
    }

    @Override
    public int getMaxBufferSize()
    {
        return this.action.getMaxBufferSize();
    }

    @Nonnull
    @Override
    public ObjectData finalizeData()
    {
        return new ObjectCallbackData(this, version, stream);
    }

    @Override
    public boolean isEmpty()
    {
        return this.action.isEmpty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (!(obj instanceof ObjectCallbackAction))
            return false;
        ObjectCallbackAction other = (ObjectCallbackAction) obj;
        return Objects.equals(this, other);
    }
}
