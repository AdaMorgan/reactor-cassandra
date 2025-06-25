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

import java.util.EnumSet;
import java.util.stream.Collectors;

public enum ConfigFlag
{
    EVENT_PASSTHROUGH,
    SHUTDOWN_HOOK(true),
    DEBUG(true),
    AUTO_RECONNECT(true);

    private final boolean isDefault;

    public static final EnumSet<ConfigFlag> DEFAULT = EnumSet.allOf(ConfigFlag.class).stream().filter(ConfigFlag::isDefault).collect(Collectors.toCollection(() -> EnumSet.noneOf(ConfigFlag.class)));

    ConfigFlag()
    {
        this(false);
    }

    ConfigFlag(boolean isDefault)
    {
        this.isDefault = isDefault;
    }

    public boolean isDefault()
    {
        return isDefault;
    }
}
