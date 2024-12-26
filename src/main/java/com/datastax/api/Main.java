package com.datastax.api;

import com.datastax.internal.objectaction.ObjectActionImpl;
import com.datastax.internal.objectaction.ObjectFactoryBuilder;
import com.datastax.internal.objectaction.ObjectFactoryImpl;
import org.example.data.DataObject;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ObjectFactoryImpl factory = ObjectFactoryBuilder.create("cassandra", "cassandra")
                .withHost("127.0.0.1")
                .withPort(9042)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(50, Thread::new);

        for (int i = 0; i < 50; i++) {
            executorService.execute(() -> {
                ObjectActionImpl<List<DataObject>> clients = new ObjectActionImpl<>(factory, "SELECT * FROM system.clients", ((request, response) -> {
                    return response.getObject();
                }));

                try {
                    List<DataObject> dataObjects = clients.submit().get();

                    LoggerFactory.getLogger(Main.class).info(dataObjects.size() + " data objects");
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
