package com.datastax.api.events;

import com.datastax.api.ObjectFactory;
import org.example.data.DataObject;
import org.jetbrains.annotations.NotNull;

public abstract class Event implements GenericEvent
{
    @Override
    public @NotNull ObjectFactory getObjectFactory()
    {
        return null;
    }

    @Override
    public long getResponseNumber()
    {
        return 0;
    }

    @Override
    public DataObject getRawData()
    {
        return DataObject.empty();
    }
}
