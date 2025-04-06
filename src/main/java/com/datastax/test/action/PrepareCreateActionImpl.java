package com.datastax.test.action;

import com.datastax.api.requests.ObjectAction;
import com.datastax.api.requests.Request;
import com.datastax.api.requests.Response;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;

public class PrepareCreateActionImpl extends QueryCreateActionImpl
{
    public PrepareCreateActionImpl(LibraryImpl api, byte version, byte flags, String content, Level level, Flag... queryFlags)
    {
        super(api, version, flags, SocketCode.PREPARE, content, level, queryFlags);
    }

    @Override
    public void handleSuccess(Request<ByteBuf> request, Response response)
    {
        new ExecuteActionImpl(this.api, this.version, flags, response.getBody(), ObjectAction.Level.ONE, ObjectAction.Flag.VALUES, ObjectAction.Flag.PAGE_SIZE, ObjectAction.Flag.DEFAULT_TIMESTAMP).queue(request::onSuccess, request::onFailure);
    }
}
