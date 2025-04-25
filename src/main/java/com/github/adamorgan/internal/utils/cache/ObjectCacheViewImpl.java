package com.github.adamorgan.internal.utils.cache;

import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.cache.ObjectCacheView;

public class ObjectCacheViewImpl extends AbstractCacheViewImpl<ObjectCreateAction> implements ObjectCacheView
{
    public ObjectCacheViewImpl(Class<ObjectCreateAction> type)
    {
        super(type);
    }
}
