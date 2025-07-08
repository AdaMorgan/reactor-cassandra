package com.github.assertions.logging;

import com.github.utils.SnapshotHandler;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingAssertions
{
    private final SnapshotHandler snapshotHandler;
    private final List<String> logs;

    public LoggingAssertions(SnapshotHandler snapshotHandler, List<String> logs)
    {
        this.snapshotHandler = snapshotHandler;
        this.logs = logs;
    }

    @Contract("->this")
    public LoggingAssertions isEmpty()
    {
        assertThat(logs).isEmpty();
        return this;
    }

    @Contract("  -> this")
    public LoggingAssertions matchesSnapshot()
    {
        return matchesSnapshot(null);
    }

    @Contract("_ -> this")
    public LoggingAssertions matchesSnapshot(String suffix)
    {
        this.snapshotHandler.compareWithSnapshot(String.join("\n", logs), suffix);
        return this;
    }

    @Contract("_->this")
    public LoggingAssertions containsLine(String line)
    {
        assertThat(logs).contains(line);
        return this;
    }

    @Contract("_->this")
    public LoggingAssertions doesNotContainLineMatching(Predicate<? super String> predicate)
    {
        assertThat(logs).noneMatch(predicate);
        return this;
    }
}
