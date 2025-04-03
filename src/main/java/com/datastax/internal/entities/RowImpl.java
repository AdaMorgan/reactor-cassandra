package com.datastax.internal.entities;

import com.datastax.api.entities.Column;
import com.datastax.api.entities.Row;

public class RowImpl implements Row
{
    private final Column column;
    private final Object value;

    public RowImpl(Column column, Object value)
    {
        this.column = column;
        this.value = value;
    }

    @Override
    public String toString()
    {
        return value != null ? value.toString() : "null";
    }
}
