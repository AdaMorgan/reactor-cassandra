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

package com.github.adamorgan.api.utils;

import com.github.adamorgan.api.Library;

import javax.annotation.Nonnull;

public interface SessionController
{
    /**
     * The default delay (in seconds) to wait between running {@link SessionConnectNode SessionConnectNodes}
     */
    int IDENTIFY_DELAY = 5;

    default void setConcurrency(int level) {}

    void appendSession(@Nonnull SessionConnectNode node);

    void removeSession(@Nonnull SessionConnectNode node);

    interface SessionConnectNode
    {
        @Nonnull
        Library getLibrary();

        /**
         * The {@link com.github.adamorgan.api.Library.ShardInfo ShardInfo} for this request.
         * <br>Can be used for a priority system.
         *
         * @return The ShardInfo
         */
        @Nonnull
        Library.ShardInfo getShardInfo();

        void run(boolean isLast) throws InterruptedException;
    }
}
