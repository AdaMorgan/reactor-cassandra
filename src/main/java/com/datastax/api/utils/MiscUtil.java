package com.datastax.api.utils;

import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Utility methods for various aspects of the API.
 */
public class MiscUtil
{
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