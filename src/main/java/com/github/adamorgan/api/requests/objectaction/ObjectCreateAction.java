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

package com.github.adamorgan.api.requests.objectaction;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.requests.action.CacheObjectAction;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.request.ObjectCreateRequest;
import com.github.adamorgan.internal.utils.Checks;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface ObjectCreateAction extends ObjectAction<Response>, ObjectCreateRequest<ObjectCreateAction>, CacheObjectAction<Response>
{
    @Nonnull
    Compression getCompression();
}
