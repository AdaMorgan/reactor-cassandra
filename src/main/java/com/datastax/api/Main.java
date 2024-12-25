package com.datastax.api;

import com.datastax.internal.objectaction.ObjectActionImpl;
import com.datastax.internal.objectaction.ObjectFactoryBuilder;
import com.datastax.internal.objectaction.ObjectFactoryImpl;
import org.example.data.DataObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) {
        ObjectFactoryImpl factory = ObjectFactoryBuilder.create("cassandra", "cassandra")
                .withHost("127.0.0.1")
                .withPort(19042)
                .build();

        ObjectActionImpl<List<DataObject>> clients = new ObjectActionImpl<>(factory, "SELECT * FROM system.clients", ((request, response) -> {
            return response.getObject();
        }));

        try {
            clients.submit().get().forEach(System.out::println);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
