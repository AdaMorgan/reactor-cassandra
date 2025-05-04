package com.github.adamorgan.test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

public class RowImpl
{
    private final ColumnImpl column;
    private final Serializable value;

    public RowImpl(@Nonnull ColumnImpl column, @Nullable Serializable value)
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
