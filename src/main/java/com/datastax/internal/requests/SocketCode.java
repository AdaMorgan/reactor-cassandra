package com.datastax.internal.requests;

public final class SocketCode
{
    public static final int ERROR = 0x00;
    public static final int STARTUP = 0x01;
    public static final int READY = 0x02;
    public static final int AUTHENTICATE = 0x03;
    public static final int OPTIONS = 0x05;
    public static final int SUPPORTED = 0x06;
    public static final int QUERY = 0x07;
    public static final int RESULT = 0x08;
    public static final int PREPARE = 0x09;
    public static final int EXECUTE = 0x0A;
    public static final int REGISTER = 0x0B;
    public static final int EVENT = 0x0C;
    public static final int BATCH = 0x0D;
    public static final int AUTH_CHALLENGE = 0x0E;
    public static final int AUTH_RESPONSE = 0x0F;
    public static final int AUTH_SUCCESS = 0x10;
}
