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

import com.github.adamorgan.api.requests.Response;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

public class ErrorResponseException extends RuntimeException
{
    private final ErrorResponse errorResponse;
    private final ByteBuf response;
    private final int code;
    private final String meaning;

    public ErrorResponseException(ErrorResponse errorResponse, ByteBuf response, int code, String meaning)
    {
        super(meaning);
        this.errorResponse = errorResponse;
        this.response = response;
        this.code = code;
        this.meaning = meaning;
    }

    /**
     * The {@link ErrorResponse ErrorResponse} corresponding
     * for the received error response from CQL Binary Protocol
     *
     * @return {@link ErrorResponse ErrorResponse}
     */
    @Nonnull
    public ErrorResponse getErrorResponse()
    {
        return errorResponse;
    }

    public int getErrorCode()
    {
        return code;
    }

    @Nonnull
    public String getMeaning()
    {
        return meaning;
    }

    @Nonnull
    public static ErrorResponseException create(ErrorResponse errorResponse, Response response)
    {
        return create(errorResponse, response.getBody());
    }

    @Nonnull
    public static ErrorResponseException create(ErrorResponse errorResponse, ByteBuf response)
    {
        int length = response.readUnsignedShort();
        String meaning = response.readCharSequence(length, StandardCharsets.UTF_8).toString();
        return new ErrorResponseException(errorResponse, response, errorResponse.getCode(), meaning);
    }
}
