package com.datastax.api.entities;

import com.datastax.api.CustomInfo;
import com.datastax.api.utils.data.DataType;

import java.io.Serializable;

public interface Column
{
    String getName();

    DataType getType();
}
