import com.datastax.api.LibraryBuilder;
import com.datastax.api.events.ExceptionEvent;
import com.datastax.api.events.ShutdownEvent;
import com.datastax.api.events.StatusChangeEvent;
import com.datastax.api.hooks.ListenerAdapter;
import com.datastax.internal.LibraryImpl;
import org.apache.http.conn.ConnectionPoolTimeoutException;

import javax.annotation.Nonnull;
import java.net.UnknownHostException;

public final class SessionLoggerExample extends ListenerAdapter
{

    public static void main(String[] args) throws ConnectionPoolTimeoutException
    {
        LibraryImpl api = LibraryBuilder.createLight()
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
    public void onShutdown(@Nonnull ShutdownEvent event)
    {
        LibraryImpl.LOG.info("Shutting down...");
    }
}
