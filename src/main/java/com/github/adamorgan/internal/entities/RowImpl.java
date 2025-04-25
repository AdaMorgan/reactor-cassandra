package com.github.adamorgan.internal.entities;

import com.github.adamorgan.api.entities.Column;
import com.github.adamorgan.api.entities.Row;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RowImpl implements Row
{
    private final Column column;
    private final Object value;

    public RowImpl(@Nonnull Column column, @Nullable Object value)
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
