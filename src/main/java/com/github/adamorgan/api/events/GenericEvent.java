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

package com.github.adamorgan.api.events;

import com.github.adamorgan.api.Library;

import javax.annotation.Nonnull;

public interface GenericEvent
{
    /**
     * The current {@link Library} instance corresponding to this Event
     *
     * @return The corresponding {@link Library} instance
     */
    @Nonnull
    Library getLibrary();
    
    /**
     * The current sequence for this event.
     * <br>This can be used to keep events in order when making sequencing system.
     *
     * @return The current sequence number for this event
     */
    long getResponseNumber();
}
