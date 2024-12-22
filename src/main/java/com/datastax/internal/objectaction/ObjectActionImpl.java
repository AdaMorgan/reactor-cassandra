package com.datastax.internal.objectaction;

import org.example.data.SerializableData;

import java.util.function.BiFunction;

public class ObjectActionImpl<T> implements ObjectAction<T> {
    public ObjectActionImpl(String route, BiFunction<Request<T>, Response, SerializableData> handler) {

    }
}