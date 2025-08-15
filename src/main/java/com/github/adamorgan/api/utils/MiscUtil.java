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

import com.github.adamorgan.annotations.UnknownNullability;
import com.github.adamorgan.api.requests.Request;
import com.github.adamorgan.api.requests.RequestManager;
import com.github.adamorgan.api.utils.request.ObjectData;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Utility methods for various aspects of the API.
 */
public class MiscUtil
{
    public static int getBucketId(ByteBuf request)
    {
        int hash = request.hashCode();
        hash = (hash ^ (hash >>> 16)) * 0x5BD1E995;
        return (int) (((hash & 0x7FFFFFFFL) * ((1 << 15) - 1)) >>> 31);
    }

    @UnknownNullability
    public static <E> E locked(ReentrantLock lock, Supplier<E> task)
    {
        tryLock(lock);
        try
        {
            return task.get();
        }
        finally
        {
            lock.unlock();
        }
    }

    public static void locked(ReentrantLock lock, Runnable task)
    {
        tryLock(lock);
        try
        {
            task.run();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Tries to acquire the provided lock in a 10 second timeframe.
     *
     * @param lock The lock to acquire
     * @throws IllegalStateException If the lock could not be acquired
     */
    public static void tryLock(Lock lock)
    {
        try
        {
            if (!lock.tryLock() && !lock.tryLock(10, TimeUnit.SECONDS))
            {
                throw new IllegalStateException("Could not acquire lock in a reasonable timeframe! (10 seconds)");
            }
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException("Unable to acquire lock while thread is interrupted!");
        }
    }
}