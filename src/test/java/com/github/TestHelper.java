package com.github;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.adamorgan.internal.utils.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestHelper
{
    public static byte[] readFully(InputStream stream) throws IOException
    {
        Checks.notNull(stream, "InputStream");

        byte[] buffer = new byte[1024];
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream())
        {
            int readAmount = 0;
            while ((readAmount = stream.read(buffer)) != -1)
            {
                bos.write(buffer, 0, readAmount);
            }
            return bos.toByteArray();
        }
    }

    public static List<String> captureLogging(Runnable task)
    {
        return captureLogging(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME), task);
    }

    public static List<String> captureLogging(Logger logger, Runnable task)
    {
        assertThat(logger).isInstanceOf(ch.qos.logback.classic.Logger.class);
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logbackLogger.addAppender(listAppender);
        try
        {
            task.run();
            return listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
        }
        finally
        {
            logbackLogger.addAppender(listAppender);
            listAppender.stop();
        }
    }
}
