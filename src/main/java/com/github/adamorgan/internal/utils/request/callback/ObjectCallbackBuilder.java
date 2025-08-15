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

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.request.ObjectCallbackRequest;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.EncodingUtils;
import com.github.adamorgan.internal.utils.request.AbstractObjectBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ObjectCallbackBuilder extends AbstractObjectBuilder<ObjectCallbackBuilder> implements ObjectCallbackRequest<ObjectCallbackBuilder>
{
    private final ArrayList<ByteBuf> attachments = new ArrayList<>();
    protected int fields = ObjectCreateAction.Field.DEFAULT | ObjectCreateAction.Field.VALUES.getRawValue();

    @Nonnull
    @Override
    public List<? extends ByteBuf> getAttachments()
    {
        return Collections.unmodifiableList(attachments);
    }

    @Nonnull
    @Override
    public ObjectCallbackBuilder useTrace(boolean enabled)
    {
        this.traceEnabled = enabled;
        return this;
    }

    @Nonnull
    @Override
    public ObjectCallbackBuilder setContent(@Nullable String content, @Nonnull Collection<? super Serializable> args)
    {
        if (content != null)
        {
            content = content.trim();
            this.content.setLength(0);
            this.content.append(content);
        }
        else
        {
            this.content.setLength(0);
        }

        for (Object arg : args)
        {
            Serializable entry = (Serializable) arg;

            ByteBuf pack = EncodingUtils.pack(Unpooled.directBuffer(), entry);
            attachments.add(pack);
        }

        return this;
    }

    @Nonnull
    public ObjectCallbackBuilder setContent(@Nullable String content, @Nonnull Map<String, ? super Serializable> args)
    {
        if (content != null)
        {
            content = content.trim();
            this.content.setLength(0);
            this.content.append(content);
        }
        else
        {
            this.content.setLength(0);
        }

        for (Map.Entry<String, ? super Serializable> entry : args.entrySet())
        {
            if (!Field.fromBitFields(fields).contains(Field.VALUE_NAMES))
            {
                this.fields |= Field.VALUE_NAMES.getRawValue();
            }

            ByteBuf pack = EncodingUtils.pack(Unpooled.directBuffer(), entry);
            attachments.add(pack);
        }

        return this;
    }

    @Nonnull
    @Override
    public ObjectCallbackBuilder setTimestamp(long timestamp)
    {
        Checks.notNull(timestamp, "The Time");
        this.timestamp = timestamp;
        return this;
    }

    @Nonnull
    @Override
    public ObjectCallbackBuilder setConsistency(@Nonnull Consistency consistency)
    {
        Checks.notNull(consistency, "The Consistency");
        this.consistency = consistency;
        return this;
    }
}
