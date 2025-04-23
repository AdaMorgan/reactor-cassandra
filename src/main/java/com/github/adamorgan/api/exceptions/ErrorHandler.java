package com.github.adamorgan.api.exceptions;

import com.github.adamorgan.internal.utils.Checks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ErrorHandler implements Consumer<Throwable>
{
    private final Map<Predicate<? super Throwable>, Consumer<? super Throwable>> cases = new LinkedHashMap<>();
    private final Consumer<? super Throwable> base;

    public ErrorHandler(@Nonnull Consumer<? super Throwable> base)
    {
        Checks.notNull(base, "Consumer");
        this.base = base;
    }

    @Nonnull
    public ErrorHandler handle(@Nonnull ErrorResponse response, @Nonnull Consumer<? super ErrorResponseException> handler)
    {
        Checks.notNull(response, "ErrorResponse");
        return handle(EnumSet.of(response), handler);
    }

    @Nonnull
    public <T> ErrorHandler handle(@Nonnull Class<T> clazz, @Nonnull Predicate<? super T> condition, @Nonnull Consumer<? super T> handler)
    {
        Checks.notNull(clazz, "Class");
        Checks.notNull(handler, "Handler");
        return handle((it) -> clazz.isInstance(it) && condition.test(clazz.cast(it)), (ex) -> handler.accept(clazz.cast(ex)));
    }

    @Nonnull
    public ErrorHandler handle(@Nonnull Collection<Class<?>> clazz, @Nullable Predicate<? super Throwable> condition, @Nonnull Consumer<? super Throwable> handler)
    {
        Checks.noneNull(clazz, "Class");
        Checks.notNull(handler, "Handler");
        List<Class<?>> classes = new ArrayList<>(clazz);
        Predicate<? super Throwable> check = (it) -> classes.stream().anyMatch(c -> c.isInstance(it)) && (condition == null || condition.test(it));
        return handle(check, handler);
    }

    @Nonnull
    public ErrorHandler handle(@Nonnull Collection<ErrorResponse> errorResponses, @Nonnull Consumer<? super ErrorResponseException> handler)
    {
        Checks.notNull(handler, "Handler");
        Checks.noneNull(errorResponses, "ErrorResponse");
        return handle(ErrorResponseException.class, (it) -> errorResponses.contains(it.getErrorResponse()), handler);
    }

    @Nonnull
    public ErrorHandler handle(@Nonnull Predicate<? super Throwable> condition, @Nonnull Consumer<? super Throwable> handler)
    {
        Checks.notNull(condition, "Condition");
        Checks.notNull(handler, "Handler");
        cases.put(condition, handler);
        return this;
    }

    @Override
    public void accept(Throwable t)
    {
        for (Map.Entry<Predicate<? super Throwable>, Consumer<? super Throwable>> entry : cases.entrySet())
        {
            Predicate<? super Throwable> condition = entry.getKey();
            Consumer<? super Throwable> callback = entry.getValue();
            if (condition.test(t))
            {
                callback.accept(t);
                return;
            }
        }

        base.accept(t);
    }
}
