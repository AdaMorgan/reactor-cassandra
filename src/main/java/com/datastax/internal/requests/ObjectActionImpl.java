package com.datastax.internal.requests;

import com.datastax.api.Library;
import com.datastax.api.requests.ObjectAction;

public abstract class ObjectActionImpl implements ObjectAction
{
    protected final Library api;
    protected final int version;
    protected final int flags;
    protected final int stream;
    protected final int opcode;

    public ObjectActionImpl(Library api, int version, int flags, int stream, int opcode)
    {
        this.api = api;
        this.version = version;
        this.flags = flags;
        this.stream = stream;
        this.opcode = opcode;
    }
}
