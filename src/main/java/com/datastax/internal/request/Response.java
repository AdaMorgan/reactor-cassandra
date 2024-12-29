package com.datastax.internal.request;

import com.datastax.annotations.Nonnull;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.Row;
import org.example.data.DataObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Response {
    private final Stream<Row> rows;

    public Response(List<Row> rows) {
        this.rows = rows.stream();
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
        Map<String, Object> result = new HashMap<>();

        for (ColumnDefinitions.Definition definition : row.getColumnDefinitions().asList()) {
            result.put(definition.getName(), row.getObject(definition.getName()));
        }

        return result;



//        return row.getColumnDefinitions().asList().stream().collect(Collectors.toMap(key -> {
//            System.out.println(key.getName());
//            return key.getName();
//        }, value -> {
//            System.out.println(value.getName());
//            return row.getObject(value.getName());
//        }));
    }
}
