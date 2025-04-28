package com.github.adamorgan.internal.utils.cache;

import com.github.adamorgan.api.utils.cache.ObjectCacheView;
import io.netty.buffer.ByteBuf;

public class ObjectCacheViewImpl extends AbstractCacheViewImpl<ByteBuf> implements ObjectCacheView
{
    public ObjectCacheViewImpl(Class<ByteBuf> type)
    {
        super(type);
    }
}
