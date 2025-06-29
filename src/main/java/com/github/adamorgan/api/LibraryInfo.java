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

package com.github.adamorgan.api;

/**
 * Contains information to this specific build of {@value PROJECT_NAME}.
 */
public class LibraryInfo
{
    public static final byte PROTOCOL_VERSION = 4;
    public static final String PROJECT_NAME = "PROJECT_NAME";
    public static final String PROJECT_VERSION = "1.0.0";

    public static final String CQL_VERSION = "4.0.0";
    public static final String DRIVER_VERSION = "1.0.0";
    public static final String DRIVER_NAME = "driver_name";
    public static final String THROW_ON_OVERLOAD = "true";
}
