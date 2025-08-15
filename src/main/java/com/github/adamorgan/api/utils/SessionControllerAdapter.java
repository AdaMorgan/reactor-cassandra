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
import com.github.adamorgan.internal.utils.LibraryLogger;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class SessionControllerAdapter implements SessionController
{
    protected static final Logger log = LibraryLogger.getLog(SessionControllerAdapter.class);
    protected final Object lock = new Object();
    protected Queue<SessionConnectNode> connectQueue;
    protected Runnable workerHandle;
    protected long lastConnect = 0;

    public SessionControllerAdapter()
    {
        this.connectQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void appendSession(@Nonnull SessionConnectNode node)
    {
        removeSession(node);
        connectQueue.add(node);
        runWorker();
    }

    @Override
    public void removeSession(@Nonnull SessionConnectNode node)
    {
        this.connectQueue.remove(node);
    }

    protected void runWorker()
    {
        synchronized (lock)
        {
            if (workerHandle == null)
            {
                workerHandle = new QueueWorker();
                workerHandle.run();
            }
        }
    }

    protected class QueueWorker extends Thread
    {
        /** Delay (in milliseconds) to sleep between connecting sessions */
        protected final long delay;

        public QueueWorker()
        {
            this(IDENTIFY_DELAY);
        }

        /**
         * Creates a QueueWorker
         *
         * @param delay
         *        delay (in seconds) to wait between starting sessions
         */
        public QueueWorker(int delay)
        {
            this(TimeUnit.SECONDS.toMillis(delay));
        }

        /**
         * Creates a QueueWorker
         *
         * @param delay
         *        delay (in milliseconds) to wait between starting sessions
         */
        public QueueWorker(long delay)
        {
            super("SessionControllerAdapter-Worker");
            this.delay = delay;
            super.setUncaughtExceptionHandler(this::handleFailure);
        }

        protected void handleFailure(Thread thread, Throwable exception)
        {
            log.error("Worker has failed with throwable!", exception);
        }

        @Override
        public void run()
        {
            try
            {
                if (this.delay > 0)
                {
                    final long interval = System.currentTimeMillis() - lastConnect;
                    if (interval < this.delay)
                        Thread.sleep(this.delay - interval);
                }
            }
            catch (InterruptedException ex)
            {
                log.error("Unable to backoff", ex);
            }
            processQueue();
            synchronized (lock)
            {
                workerHandle = null;
                if (!connectQueue.isEmpty())
                    runWorker();
            }
        }

        protected void processQueue()
        {
            boolean isMultiple = connectQueue.size() > 1;
            while (!connectQueue.isEmpty())
            {
                SessionConnectNode node = connectQueue.poll();
                try
                {
                    node.run(isMultiple && connectQueue.isEmpty());
                    isMultiple = true;
                    lastConnect = System.currentTimeMillis();
                    if (connectQueue.isEmpty())
                        break;
                    if (this.delay > 0)
                        Thread.sleep(this.delay);
                }
                catch (IllegalStateException e)
                {
                    log.error("Unexpected exception when running connect node", e);
                    appendSession(node);
                }
                catch (InterruptedException e)
                {
                    log.error("Failed to run node", e);
                    appendSession(node);
                    return; // caller should start a new thread
                }
            }
        }
    }
}
