import com.datastax.LibraryBuilder;
import com.datastax.api.Library;
import com.datastax.api.events.GenericEvent;
import com.datastax.api.events.StatusChangeEvent;
import com.datastax.api.hooks.ListenerAdapter;
import com.datastax.internal.LibraryImpl;

import javax.annotation.Nonnull;

public final class SessionLoggerExample extends ListenerAdapter
{
    public static void main(String[] args)
    {
        Library api = LibraryBuilder.createLight()
                .addEventListeners(new SessionLoggerExample())
                .build();
    }

    @Override
    public void onStatusChangeEvent(@Nonnull StatusChangeEvent event)
    {
        LibraryImpl.LOG.info("{} -> {}", event.getOldValue(), event.getNewValue());
    }
}
