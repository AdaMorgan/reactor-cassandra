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

package com.github.adamorgan.api.exceptions;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public enum ErrorResponse
{
    SERVER_ERROR(0x0000),
    PROTOCOL_ERROR(0x000A),
    BAD_CREDENTIALS(0x0100),
    UNAVAILABLE(0x1000),
    OVERLOADED(0x1001),
    IS_BOOTSTRAPPING(0x1002),
    TRUNCATE_ERROR(0x1003),
    WRITE_TIMEOUT(0x1100),
    READ_TIMEOUT(0x1200),
    SYNTAX_ERROR(0x2000),
    UNAUTHORIZED(0x2100),
    INVALID(0x2200),
    CONFIG_ERROR(0x2300),
    ALREADY_EXISTS(0x2400),
    UNPREPARED(0x2500);

    private final int code;

    ErrorResponse(int code)
    {
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }

    @Nonnull
    public static ErrorResponse fromCode(int code)
    {
        for (ErrorResponse error : values())
        {
            if (code == error.getCode())
                return error;
        }
        return SERVER_ERROR;
    }

    @Nonnull
    public static ErrorResponse from(final ByteBuf buffer)
    {
        return ErrorResponse.fromCode(buffer.readInt());
    }
}
