package com.datastax.internal.objectaction;

public class Response {
    private final int code;

    public Response(int code) {
        this.code = code;
    }

    private boolean isError() {
        return false;
    }
}
