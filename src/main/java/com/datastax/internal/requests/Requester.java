package com.datastax.internal.requests;

import com.datastax.api.requests.Request;
import com.datastax.internal.LibraryImpl;
import com.datastax.test.StreamManager;

public class Requester
{
    private final LibraryImpl library;
    private final StreamManager streamManager;

    public Requester(LibraryImpl library)
    {
        this.library = library;
        this.streamManager = new StreamManager((short) 10);
    }

    public <R> void execute(Request<R> request)
    {
        this.library.getClient().execute(request, (short) 0x00);
    }
}
