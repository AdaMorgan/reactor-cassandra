package com.datastax.api.entities;

import com.datastax.api.utils.data.DataType;

public interface Column
{
    String getName();

    DataType getType();
}
