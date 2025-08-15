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

package com.github.adamorgan.api.requests;

import com.github.adamorgan.api.utils.request.ObjectData;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.action.ObjectActionImpl;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

public class ObjectFuture<T> extends CompletableFuture<T>
{
    protected final Request<T> request;
    protected final ObjectActionImpl<T> action;

    public ObjectFuture(ObjectActionImpl<T> action, ObjectData body, long deadline)
    {
        this.action = action;
        this.request = new Request<>(action, body, this::complete, this::completeExceptionally, deadline);

        ((LibraryImpl) this.action.getLibrary()).getRequester().request(this.request);
    }

    @Override
    public boolean cancel(final boolean mayInterrupt)
    {
        return (!isDone() && !isCancelled()) && super.cancel(mayInterrupt);
    }
}
