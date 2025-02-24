package com.datastax.internal.request;

import com.datastax.api.ObjectFactory;

public class Requester
{
    private final ObjectFactory connectionFactory;

    public Requester(ObjectFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public <R> void execute(Request<R> request)
    {
        try
        {

        }
        catch (Throwable t)
        {
            request.onFailure(t);
        }
    }

    private static class WorkTask<T>
    {
        private final Request<T> request;

        public WorkTask(Request<T> request) {
            this.request = request;
        }

        private void handleResponse()
        {
        }
    }
}
