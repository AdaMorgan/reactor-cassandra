package com.datastax.internal.requests;

import com.datastax.api.requests.Request;
import com.datastax.internal.LibraryImpl;

public class Requester
{
    private final LibraryImpl library;

    public Requester(LibraryImpl library)
    {
        this.library = library;
    }

    public <R> void execute(Request<R> request)
    {
        this.library.getClient().execute(request);
    }
}
