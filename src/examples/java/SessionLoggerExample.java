import com.github.adamorgan.api.LibraryBuilder;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.StatusChangeEvent;
import com.github.adamorgan.api.events.session.ReadyEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.action.ObjectCreateActionImpl;
import com.github.adamorgan.test.RowsResultImpl;

import javax.annotation.Nonnull;

public final class SessionLoggerExample extends ListenerAdapter
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";
    public static final String TEST_QUERY = "SELECT * FROM system.clients";

    private static final byte DEFAULT_FLAG = 0x00;

    public static void main(String[] args)
    {
        LibraryImpl api = LibraryBuilder
                .createLight("cassandra", "cassandra")
                .addEventListeners(new SessionLoggerExample())
                .build();
    }

    @Override
    public void onStatusChange(@Nonnull StatusChangeEvent event)
    {
        LibraryImpl.LOG.info("{} -> {}", event.getOldStatus(), event.getNewStatus());
    }

    @Override
    public void onException(@Nonnull ExceptionEvent event)
    {
        if (!event.isLogged())
        {
            LibraryImpl.LOG.warn(event.getCause().getMessage());
        }
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event)
    {
        LibraryImpl api = (LibraryImpl) event.getLibrary();

        api.sendRequest(TEST_QUERY).map(RowsResultImpl::new).queue(System.out::println, Throwable::printStackTrace);

        api.sendRequest(TEST_QUERY_PREPARED, 123546L, "user").map(RowsResultImpl::new).queue(System.out::println, Throwable::printStackTrace);
    }

    @Override
    public void onShutdown(@Nonnull ShutdownEvent event)
    {
        LibraryImpl.LOG.info("Shutting down...");
    }
}
