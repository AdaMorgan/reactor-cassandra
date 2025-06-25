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

package com.github.adamorgan.internal.requests;

public final class SocketCode
{
    public static final byte ERROR = 0x00;
    public static final byte STARTUP = 0x01;
    public static final byte READY = 0x02;
    public static final byte AUTHENTICATE = 0x03;
    public static final byte OPTIONS = 0x05;
    public static final byte SUPPORTED = 0x06;
    public static final byte QUERY = 0x07;
    public static final byte RESULT = 0x08;
    public static final byte PREPARE = 0x09;
    public static final byte EXECUTE = 0x0A;
    public static final byte REGISTER = 0x0B;
    public static final byte EVENT = 0x0C;
    public static final byte BATCH = 0x0D;
    public static final byte AUTH_CHALLENGE = 0x0E;
    public static final byte AUTH_RESPONSE = 0x0F;
    public static final byte AUTH_SUCCESS = 0x10;
}
