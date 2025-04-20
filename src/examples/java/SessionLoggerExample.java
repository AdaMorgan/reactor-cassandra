import com.datastax.api.LibraryBuilder;
import com.datastax.api.events.ExceptionEvent;
import com.datastax.api.events.StatusChangeEvent;
import com.datastax.api.events.session.ReadyEvent;
import com.datastax.api.events.session.ShutdownEvent;
import com.datastax.api.hooks.ListenerAdapter;
import com.datastax.api.requests.objectaction.ObjectCreateAction;
import com.datastax.internal.LibraryImpl;
import com.datastax.test.RowsResultImpl;
import com.datastax.test.action.ObjectCreateActionImpl;
import com.datastax.test.action.ObjectCreateActionTest;
import com.datastax.test.action.PrepareCreateActionImpl;

import javax.annotation.Nonnull;

public final class SessionLoggerExample extends ListenerAdapter
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";
    public static final String TEST_QUERY = "SELECT * FROM system.clients";

    private static final byte DEFAULT_FLAG = 0x00;

    public static void main(String[] args)
    {
        LibraryImpl api = LibraryBuilder.createLight("cassandra", "cassandra")
                .addEventListeners(new SessionLoggerExample())
                .build();

        api.getClient().connect();
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

        new ObjectCreateActionImpl(api, DEFAULT_FLAG, TEST_QUERY, ObjectCreateAction.Consistency.ONE)
                .map(RowsResultImpl::new)
                .queue(System.out::println, Throwable::printStackTrace);

        new PrepareCreateActionImpl(api, DEFAULT_FLAG, TEST_QUERY_PREPARED, ObjectCreateAction.Consistency.ONE)
                .map(RowsResultImpl::new)
                .queue(System.out::println, Throwable::printStackTrace);

//        new ObjectCreateActionTest(api, DEFAULT_FLAG, ObjectCreateAction.Consistency.ONE)
//                .setContent(TEST_QUERY_PREPARED)
//                .addValues(123456, "user")
//                .queue(System.out::println, Throwable::printStackTrace);
    }

    @Override
    public void onShutdown(@Nonnull ShutdownEvent event)
    {
        LibraryImpl.LOG.info("Shutting down...");
    }
}
