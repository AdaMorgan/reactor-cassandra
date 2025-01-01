package com.datastax.api;

import com.datastax.api.sharding.DefaultObjectManagerBuilder;

import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) {
        DefaultObjectManagerBuilder.create("cassandra", "cassandra", (username, password) -> {
            return new InetSocketAddress("127.0.0.1", 9042);
        }).build();


//        ExecutorService executorService = Executors.newFixedThreadPool(50, Thread::new);
//
//        executorService.execute(() -> {
//            new ObjectActionImpl<List<DataObject>>(factory, "SELECT * FROM system.clients", ((request, response) -> {
//                return response.getObject();
//            })).queue(success -> {
//                System.out.println(success.size());
//            });
//        });
    }
}
