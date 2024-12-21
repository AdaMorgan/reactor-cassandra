package com.datastax;

import com.datastax.driver.core.Cluster;

public class Main {
    public static void main(String[] args) {
        Cluster cluster = new Cluster.Builder()
                .addContactPoint("127.0.0.1")
                .withPort(9042)
                .build()
                .init();

        cluster.connect().close();

        System.out.println("Connected to cluster");
    }
}