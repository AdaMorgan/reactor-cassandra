package com.github.adamorgan.api.requests.objectaction;

import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.request.ObjectRequest;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;

public interface ObjectCallbackAction extends ObjectAction<Response>, ObjectRequest<ObjectCallbackAction>
{
    @Nonnull
    ByteBuf getToken();

    @Nonnull
    Compression getCompression();
}
