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

package com.github.adamorgan.api.requests.action;

import com.github.adamorgan.api.requests.ObjectAction;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

public interface CacheObjectAction<T> extends ObjectAction<T>
{
    /**
     * Sets whether this request should rely on cached entities, or always retrieve a new one.
     *
     * @param  useCache
     *         True if the cache should be used when available, even if the entity might be outdated.
     *         False, to always request a new instance from the CQL Binary Protocol.
     *
     * @return This ObjectAction instance
     */
    @Nonnull
    @CheckReturnValue
    CacheObjectAction<T> useCache(boolean useCache);
}
