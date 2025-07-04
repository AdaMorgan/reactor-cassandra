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

import com.github.adamorgan.api.requests.objectaction.ObjectCallbackAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.SocketCode;
import com.github.adamorgan.internal.requests.action.ObjectCallbackActionImpl;
import com.github.adamorgan.internal.utils.UnlockHook;
import io.netty.buffer.*;
import io.netty.util.ReferenceCountUtil;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public class ObjectCallbackData implements ObjectData
{
    private final ObjectCallbackActionImpl action;
    private final byte version, opcode;
    private final int flags;
    private final int id;
    private final ByteBuf token, header, body, rawBody;
    private final short consistency;
    private final int fields, maxBufferSize;
    private final long nonce;

    public ObjectCallbackData(@Nonnull ObjectCallbackAction action)
    {
        this.action = (ObjectCallbackActionImpl) action;
        this.version = this.action.version;
        this.flags = action.getRawFlags();
        this.id = ((LibraryImpl) action.getLibrary()).getRequester().poll();
        this.opcode = SocketCode.EXECUTE;
        this.token = action.useCache() ? action.getLibrary().getObjectCache().cache(action.hashCode(), action.getToken()) : action.getToken();
        this.consistency = action.getConsistency().getCode();
        this.fields = action.getFieldsRaw();
        this.maxBufferSize = action.getMaxBufferSize();
        this.nonce = action.getNonce();

        this.rawBody = applyBody();
        this.body = action.getCompression().pack(rawBody);
        this.header = applyHeader();
    }

    @Override
    public int getId()
    {
        return id;
    }

    @Nonnull
    @Override
    public EnumSet<ObjectCreateAction.Field> getFields()
    {
        return ObjectCreateAction.Field.fromBitFields(fields);
    }

    @Nonnull
    @Override
    public ByteBuf applyData()
    {
        return Unpooled.compositeBuffer(2).addComponents(true, header, body);
    }

    @Nonnull
    private ByteBuf applyHeader()
    {
        return Unpooled.directBuffer()
                .writeByte(this.version)
                .writeByte(this.flags)
                .writeShort(this.id)
                .writeByte(this.opcode)
                .writeInt(body.readableBytes());
    }

    @Nonnull
    private ByteBuf applyBody()
    {
        return Unpooled.directBuffer()
                .writeShort(this.token.readableBytes())
                .writeBytes(this.token)
                .writeShort(this.consistency)
                .writeByte(this.fields)
                .writeBytes(action.getBody())
                .writeInt(this.maxBufferSize)
                .writeLong(this.nonce);
    }
}
