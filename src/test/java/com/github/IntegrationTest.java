package com.github;

import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.Requester;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import java.util.concurrent.ScheduledExecutorService;

public class IntegrationTest extends AbstractSnapshotTest
{
    @Mock
    protected LibraryImpl api;
    @Mock
    protected Requester requester;
    @Mock
    protected ScheduledExecutorService scheduledExecutorService;

    private AutoCloseable closeable;
    private int expectedRequestCount;

    @BeforeEach
    protected final void setup()
    {

    }
}
