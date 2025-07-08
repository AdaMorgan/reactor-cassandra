package com.github;

import com.github.assertions.logging.LoggingAssertions;
import com.github.utils.SnapshotHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.List;

import static com.github.TestHelper.captureLogging;

public class AbstractSnapshotTest
{
    protected SnapshotHandler snapshotHandler;

    @BeforeEach
    void initializeSnapshotHandler(TestInfo testInfo)
    {
        this.snapshotHandler = new SnapshotHandler(testInfo);
    }

    @Nonnull
    @CheckReturnValue
    protected LoggingAssertions assertThatLoggingFrom(Runnable runnable)
    {
        List<String> logs = captureLogging(runnable);
        return new LoggingAssertions(snapshotHandler, logs);
    }
}
