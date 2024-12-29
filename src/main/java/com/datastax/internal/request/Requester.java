package com.datastax.internal.request;

import com.datastax.driver.core.Row;
import com.datastax.api.ObjectFactory;
import com.datastax.internal.ObjectFactoryImpl;

import java.util.List;

public class Requester
{
    private final ObjectFactory connectionFactory;

    public Requester(ObjectFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public <R> void execute(Request<R> request)
    {
        List<Row> execute = ((ObjectFactoryImpl) connectionFactory).execute(request.getRoute());
        new WorkTask<>(request).handleResponse(execute);
    }

    private static class WorkTask<T>
    {
        private final Request<T> request;

        public WorkTask(Request<T> request) {
            this.request = request;
        }

        private void handleResponse(List<Row> rows)
        {
            this.request.handleResponse(new Response(rows));
        }
    }
}
