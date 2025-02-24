package com.datastax.internal.request;

import com.datastax.annotations.Nonnull;
import org.example.data.DataObject;

import java.util.List;
import java.util.stream.Stream;

public final class Response {
    private final Stream<String> rows;

    public Response(List<String> rows) {
        this.rows = rows.stream();
    }

    @Nonnull
    public List<DataObject> getObject()
    {
        //return this.rows.map(this::getObject).map(DataObject::new).collect(Collectors.toList());
        return null;
    }

}
