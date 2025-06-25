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

package com.github.adamorgan.internal.requests.action.operator;

import com.github.adamorgan.api.requests.ObjectAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapObjectAction<I, O> extends ObjectActionOperator<I, O>
{
    protected final Function<? super I, ? extends O> map;

    public MapObjectAction(ObjectAction<I> action, Function<? super I, ? extends O> map)
    {
        super(action);
        this.map = map;
    }

    @Override
    public void queue(@Nullable Consumer<? super O> success, @Nullable Consumer<? super Throwable> failure)
    {
        handle(action, failure, (result) -> doSuccess(success, map.apply(result)));
    }

    @Override
    public O complete(boolean shouldQueue)
    {
        return map.apply(action.complete(shouldQueue));
    }

    @Nonnull
    @Override
    public CompletableFuture<O> submit(boolean shouldQueue)
    {
        return action.submit(shouldQueue).thenApply(map);
    }
}
