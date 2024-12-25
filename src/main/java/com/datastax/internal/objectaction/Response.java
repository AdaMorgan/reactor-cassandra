package com.datastax.internal.objectaction;

import com.datastax.annotations.Nonnull;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.Row;
import org.example.data.DataObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Response {
    private final Stream<Row> rows;
    private final int code;

    public Response(List<Row> rows, int code) {
        this.rows = rows.stream();
        this.code = code;
    }

    @Nonnull
    public List<DataObject> getObject() {
        return this.rows.map(this::getObject).map(DataObject::new).collect(Collectors.toList());
    }

    @Nonnull
    public List<Map<String, Object>> getMap() {
        return this.rows.map(this::getObject).collect(Collectors.toList());
    }

    @Nonnull
    private Map<String, Object> getObject(Row row) {
        return row.getColumnDefinitions().asList().stream().collect(Collectors.toMap(ColumnDefinitions.Definition::getName, value -> row.getObject(value.getName())));
    }
}
