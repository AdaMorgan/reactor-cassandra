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

import com.github.adamorgan.api.Library;
import com.github.adamorgan.internal.requests.Requester;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicLong;

public interface RequestManager
{

    void enqueue(@Nonnull Requester.WorkTask task);

    /**
     * Indication to stop accepting new requests.
     *
     * @param shutdown
     *        Whether to also cancel previously queued request
     * @param callback
     *        Function to call once all requests are completed, used for final cleanup
     */
    void stop(boolean shutdown, @Nonnull Runnable callback);

    /**
     * Whether the queue has stopped accepting new requests.
     *
     * @return True, if the queue is stopped
     */
    boolean isStopped();

    int cancelRequests();

    interface Work
    {
        @Nonnull
        Library getLibrary();

        boolean isSkipped();

        boolean isDone();

        boolean isPriority();

        boolean isCancelled();

        void cancel();
    }

    interface GlobalRateLimit
    {
        long getClassic();

        void setClassic(long timestamp);

        @Nonnull
        static GlobalRateLimit create()
        {
            return new GlobalRateLimit()
            {
                private final AtomicLong classic = new AtomicLong(-1);
                private final AtomicLong cloudflare = new AtomicLong(-1);

                @Override
                public long getClassic()
                {
                    return classic.get();
                }

                @Override
                public void setClassic(long timestamp)
                {
                    classic.set(timestamp);
                }
            };
        }
    }

    class RateLimitConfig
    {

    }
}
