import com.github.adamorgan.api.LibraryBuilder;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.StatusChangeEvent;
import com.github.adamorgan.api.events.session.ReadyEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.binary.BinaryArray;
import com.github.adamorgan.internal.LibraryImpl;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class SessionLoggerExample extends ListenerAdapter
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM system_auth.demo WHERE user_id = :user_id AND username = :username";
    public static final String TEST_QUERY_WARNING = "SELECT * FROM system_auth.demo WHERE username = :username ALLOW FILTERING";
    public static final String TEST_QUERY = "SELECT * FROM system_traces.all_types";

    public static void main(String[] args)
    {
        InetSocketAddress address = InetSocketAddress.createUnresolved("127.0.0.1", 9042);

        LibraryImpl api = LibraryBuilder.createLight(address, "cassandra", "cassandra")
                .addEventListeners(new SessionLoggerExample())
                .setCompression(Compression.SNAPPY)
                .setEnableDebug(false)
                .build();

        //new SessionLoggerExample().testResourceLeakDetector(api);
    }

    @Override
    public void onStatusChange(@Nonnull StatusChangeEvent event)
    {
        LibraryImpl.LOG.info("{} -> {}", event.getOldStatus(), event.getNewStatus());
    }

    @Override
    public void onException(@Nonnull ExceptionEvent event)
    {

    }

    @Override
    public void onReady(@Nonnull ReadyEvent event)
    {
        LibraryImpl api = (LibraryImpl) event.getLibrary();

        api.sendRequest(TEST_QUERY).map(array -> array).queue(System.out::println, error -> System.out.println(error.getMessage()));

        Collection<Serializable> parameters = new ArrayList<>();
        parameters.add(844613816943771649L);
        parameters.add("reganjohn");

        //api.sendRequest(TEST_QUERY_WARNING, "reganjohn").map(array -> array).queue(System.out::println, Throwable::printStackTrace);

        //api.sendRequest(TEST_QUERY_PREPARED, parameters).map(RowsResultImpl::new).queue(System.out::println, Throwable::printStackTrace);

        Map<String, Serializable> map = new HashMap<>();
        map.put("user_id", 844613816943771649L);
        map.put("username", "reganjohn");

        //api.sendRequest(TEST_QUERY_PREPARED, map).map(RowsResultImpl::new).queue(System.out::println, Throwable::printStackTrace);
    }

    @Override
    public void onShutdown(@Nonnull ShutdownEvent event)
    {
        LibraryImpl.LOG.info("Shutting down...");
    }

    public void testResourceLeakDetector(LibraryImpl api)
    {
        Collection<Serializable> parameters = new ArrayList<>();
        parameters.add(844613816943771649L);
        parameters.add("reganjohn");

        long startTime = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger();
        final int count = 25000;

        Consumer<BinaryArray> result = (rowsResult) -> {
            counter.incrementAndGet();
            if (counter.get() == count)
            {
                System.out.println(System.currentTimeMillis() - startTime);
            }
        };

        for (int i = 0; i < count; i++)
        {
            api.sendRequest(TEST_QUERY_PREPARED, parameters).queue(result, Throwable::printStackTrace);
        }
    }
}
