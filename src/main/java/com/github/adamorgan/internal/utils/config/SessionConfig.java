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

package com.github.adamorgan.internal.utils.config;

import com.github.adamorgan.api.utils.ConcurrentSessionController;
import com.github.adamorgan.api.utils.ConfigFlag;
import com.github.adamorgan.api.utils.SessionController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;

public class SessionConfig
{
    protected final SessionController controller;
    protected final int maxBufferSize;
    protected final int maxReconnectDelay;
    protected final EnumSet<ConfigFlag> flags;

    public SessionConfig(@Nullable SessionController controller, int maxBufferSize, int maxReconnectDelay, EnumSet<ConfigFlag> flags)
    {
        this.controller = controller == null ? new ConcurrentSessionController() : controller;
        this.maxBufferSize = maxBufferSize;
        this.maxReconnectDelay = maxReconnectDelay;
        this.flags = flags;
    }

    @Nonnull
    public SessionController getSessionController()
    {
        return controller;
    }

    public int getMaxBufferSize()
    {
        return maxBufferSize;
    }

    public int getMaxReconnectDelay()
    {
        return maxReconnectDelay;
    }

    public boolean isEventPassthrough()
    {
        return flags.contains(ConfigFlag.EVENT_PASSTHROUGH);
    }

    public boolean isDebug()
    {
        return flags.contains(ConfigFlag.DEBUG);
    }

    public boolean isUseShutdownHook()
    {
        return flags.contains(ConfigFlag.SHUTDOWN_HOOK);
    }
    
    public boolean isAutoReconnect()
    {
        return flags.contains(ConfigFlag.AUTO_RECONNECT);
    }
}
