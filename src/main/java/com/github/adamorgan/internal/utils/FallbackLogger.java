package com.github.adamorgan.internal.utils;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.time.LocalDateTime;

public class FallbackLogger extends LegacyAbstractLogger
{
    private final String name;

    public FallbackLogger(String name)
    {
        this.name = name;
    }

    @Override
    protected String getFullyQualifiedCallerName()
    {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable)
    {
        LocalDateTime now = LocalDateTime.now();
        FormattingTuple result = MessageFormatter.arrayFormat(messagePattern, arguments);
        System.err.printf("%1$tF %1$tT [%2$s] [%3$s] %4$s%n", now, name, level, result.getMessage());
        if (throwable != null)
            throwable.printStackTrace(System.err);
    }

    @Override
    public boolean isTraceEnabled()
    {
        return false;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return true;
    }

    @Override
    public boolean isInfoEnabled()
    {
        return true;
    }

    @Override
    public boolean isWarnEnabled()
    {
        return true;
    }

    @Override
    public boolean isErrorEnabled()
    {
        return true;
    }
}

