package com.github.adamorgan.internal.utils;

import javax.annotation.Nonnull;

/**
 * Iteration procedure accepting one argument and returning whether to continue iteration.
 *
 * @param <T> The type of the argument
 */
@FunctionalInterface
public interface Procedure<T>
{
    boolean execute(@Nonnull T value);
}
