package com.github.adamorgan.api.entities;

import javax.annotation.Nonnull;

public interface Column
{
    @Nonnull
    String getName();

    int getType();
}
