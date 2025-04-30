package com.github.adamorgan.test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RowImpl
{
    private final ColumnImpl column;
    private final Object value;

    public RowImpl(@Nonnull ColumnImpl column, @Nullable Object value)
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
