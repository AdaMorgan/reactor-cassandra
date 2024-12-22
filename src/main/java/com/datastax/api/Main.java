package com.datastax.api;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class Main {

    public static void main(String[] args) {
        Session connect = new Cluster.Builder()
                .addContactPoints("127.0.0.1")
                .withPort(9042)
                .build()
                .connect();

    }
}
