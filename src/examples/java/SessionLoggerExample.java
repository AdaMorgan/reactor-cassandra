import com.github.adamorgan.api.LibraryBuilder;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.StatusChangeEvent;
import com.github.adamorgan.api.events.session.ReadyEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.test.RowsResultImpl;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class SessionLoggerExample extends ListenerAdapter
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM demo.test WHERE user_id = :user_id AND user_name = :user_name";
    public static final String TEST_QUERY_WARNING = "SELECT * FROM demo.test WHERE global_name = :global_name ALLOW FILTERING";
    public static final String TEST_QUERY = "SELECT * FROM system.clients ALLOW FILTERING";

    public static void main(String[] args)
    {
        InetSocketAddress address = InetSocketAddress.createUnresolved("127.0.0.1", 9042);

        LibraryImpl api = LibraryBuilder.createLight(address, "cassandra", "cassandra")
                .addEventListeners(new SessionLoggerExample())
                .setCompression(Compression.LZ4)
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

        Collection<Serializable> parameters = new ArrayList<>();
        parameters.add(123456L);
        parameters.add("reganjohn");

        api.sendRequest(TEST_QUERY_WARNING, "John Regan").map(RowsResultImpl::new).queue(System.out::println, Throwable::printStackTrace);

        api.sendRequest(TEST_QUERY_PREPARED, parameters).map(RowsResultImpl::new).queue(System.out::println, Throwable::printStackTrace);

        Map<String, Serializable> map = new HashMap<>();
        map.put("user_id", 123456L);
        map.put("user_name", "reganjohn");

        //api.sendRequest(TEST_QUERY_PREPARED, map).map(RowsResultImpl::new).queue(System.out::println, Throwable::printStackTrace);
    }

    @Override
    public void onShutdown(@Nonnull ShutdownEvent event)
    {
        LibraryImpl.LOG.info("Shutting down...");
    }
}
