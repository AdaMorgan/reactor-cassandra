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

package com.github.adamorgan.api.events.binary;

import com.github.adamorgan.api.events.Event;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.Response;
import io.netty.buffer.ByteBuf;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BinaryRequestEvent extends Event
{
    private final Request<?> request;
    private final Response response;

    public BinaryRequestEvent(@Nonnull final Request<?> request, @Nonnull final Response response)
    {
        super(request.getLibrary());
        this.request = request;
        this.response = response;
    }

    @Nonnull
    public Request<?> getRequest()
    {
        return request;
    }

    @Nullable
    public ByteBuf getRequestBody()
    {
        return !this.api.isDebug() ? this.request.getBody() : null;
    }

    @Nonnull
    public Response getResponse()
    {
        return response;
    }

    @Nonnull
    public ByteBuf getResponseBody()
    {
        return this.response.getBody();
    }

    @Nonnull
    @CheckReturnValue
    public ObjectAction<?> getObjectAction()
    {
        return this.request.getObjectAction();
    }
}
