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
import com.github.adamorgan.api.utils.request.ObjectCreateRequest;
import com.github.adamorgan.api.utils.request.ObjectRequest;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.Helpers;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ObjectCreateBuilder extends AbstractObjectBuilder<ObjectCreateBuilder> implements ObjectCreateRequest<ObjectCreateBuilder>
{
    @Nonnull
    @Override
    public ObjectCreateBuilder setContent(@Nullable String content)
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
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder useTrace(boolean enabled)
    {
        this.traceEnabled = enabled;
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder addContent(@Nonnull String content)
    {
        Checks.notNull(content, "Content");
        this.content.append(content);
        return this;
    }

    @Nonnull
    @Override
    public ObjectCreateBuilder setConsistency(@Nonnull ObjectCreateAction.Consistency consistency)
    {
        Checks.notNull(consistency, "Consistency");
        this.consistency = consistency;
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setMaxBufferSize(int bufferSize)
    {
        Checks.notNegative(bufferSize, "The buffer size");
        this.maxBufferSize = bufferSize;
        return this;
    }

    @Nonnull
    public ObjectCreateBuilder setTimestamp(long timestamp)
    {
        Checks.notNegative(timestamp, "Timestamp");
        this.fields |= Field.DEFAULT_TIMESTAMP.getRawValue();
        this.timestamp = timestamp;
        return this;
    }
}
