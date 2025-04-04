import com.datastax.api.LibraryBuilder;
import com.datastax.api.events.StatusChangeEvent;
import com.datastax.api.hooks.ListenerAdapter;
import com.datastax.internal.LibraryImpl;
import com.datastax.test.SocketClient;

import javax.annotation.Nonnull;

public final class SessionLoggerExample extends ListenerAdapter
{
    public static void main(String[] args)
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
}
