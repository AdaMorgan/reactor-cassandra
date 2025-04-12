package com.datastax.api.exceptions;

import com.datastax.api.requests.Response;
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

    public String getMeaning()
    {
        return meaning;
    }

    public static ErrorResponseException create(ErrorResponse errorResponse, Response response)
    {
        int length = response.getBody().readUnsignedShort();
        String meaning = response.getBody().readCharSequence(length, StandardCharsets.UTF_8).toString();
        return new ErrorResponseException(errorResponse, response.getBody(), errorResponse.getCode(), meaning);
    }
}
