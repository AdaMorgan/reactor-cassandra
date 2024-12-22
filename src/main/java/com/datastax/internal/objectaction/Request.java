package com.datastax.internal.objectaction;

import com.datastax.annotations.Nonnull;
import org.example.data.DataObject;

public class Request<T> {
    private final DataObject rawData;

    public Request(ObjectActionImpl<T> action, DataObject rawData) {
        this.rawData = rawData;
    }

    @Nonnull
    public DataObject getRawData() {
        return rawData;
    }
}
