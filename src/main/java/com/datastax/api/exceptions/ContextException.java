package com.datastax.api.exceptions;


import com.datastax.internal.utils.Helpers;

import java.util.function.Consumer;

/**
 * Used to pass a context to async exception handling for debugging purposes.
 */
public class ContextException extends Exception
{
    /**
     * Creates a failure consumer that appends a context cause
     * before printing the stack trace using {@link Throwable#printStackTrace()}.
     * <br>Equivalent to {@code here(Throwable::printStackTrace)}
     *
     * @return Wrapping failure consumer around {@code Throwable::printStackTrace}
     */
    public static Consumer<Throwable> herePrintingTrace()
    {
        return here(Throwable::printStackTrace);
    }

    /**
     * Creates a wrapping {@link java.util.function.Consumer Consumer} for
     * the provided target.
     *
     * @param  acceptor
     *         The end-target for the throwable
     *
     * @return Wrapper of the provided consumer that will append a context with the current stack-trace
     */
    public static Consumer<Throwable> here(Consumer<? super Throwable> acceptor)
    {
        return new ContextConsumer(new ContextException(), acceptor);
    }

    /**
     * Wrapper for a failure {@link Consumer} that carries a {@link ContextException} as cause.
     */
    public static class ContextConsumer implements Consumer<Throwable>
    {
        private final ContextException context;
        private final Consumer<? super Throwable> callback;

        private ContextConsumer(ContextException context, Consumer<? super Throwable> callback)
        {
            this.context = context;
            this.callback = callback;
        }

        @Override
        public void accept(Throwable throwable)
        {
            if (callback != null)
                callback.accept(Helpers.appendCause(throwable, context));
        }
    }
}
