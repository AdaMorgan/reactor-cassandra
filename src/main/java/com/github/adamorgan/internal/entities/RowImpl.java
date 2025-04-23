package com.github.adamorgan.internal.entities;

import com.github.adamorgan.api.entities.Column;
import com.github.adamorgan.api.entities.Row;

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
