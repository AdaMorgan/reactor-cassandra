package com.datastax.test;

import io.netty.buffer.ByteBuf;

public class RowsResults
{
    private final ByteBuf result;

    public RowsResults(final ByteBuf result)
    {
        this.result = result;
    }

    public void run()
    {

    }
}
