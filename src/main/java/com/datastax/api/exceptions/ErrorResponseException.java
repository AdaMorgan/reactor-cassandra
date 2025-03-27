package com.datastax.api.exceptions;

import io.netty.buffer.ByteBuf;

public class ErrorResponseException extends RuntimeException
{
    private final ErrorResponse errorResponse;
    private final ByteBuf response;
    private final int code;
    private final String message;

    public ErrorResponseException(ErrorResponse errorResponse, ByteBuf response, int code, String meaning)
    {
        super(code + ": " + meaning);
        this.errorResponse = errorResponse;
        this.response = response;
        this.code = code;
        this.message = meaning;
    }
}
