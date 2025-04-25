package com.github.adamorgan.api;

import com.github.adamorgan.api.hooks.EventListener;
import com.github.adamorgan.api.hooks.IEventManager;
import com.github.adamorgan.api.hooks.InterfacedEventManager;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.api.requests.NetworkIntent;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.config.SessionConfig;
import com.github.adamorgan.internal.utils.config.ThreadingConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LibraryBuilder
{
    protected final List<ListenerAdapter> listeners = new LinkedList<>();

    private final String username;
    private final String password;
    protected final int intents;

    protected IEventManager eventManager = null;
    protected int maxBufferSize = 2048;
    protected int maxReconnectDelay = 900;

    private LibraryBuilder(@Nullable String username, @Nullable String password, int intents)
    {
        this.username = username == null ? StringUtils.EMPTY : username;
        this.password = password == null ? StringUtils.EMPTY : password;
        this.intents = 1 | intents;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createDefault(@Nullable String username, @Nullable String password)
    {
        return createDefault(username, password, NetworkIntent.DEFAULT);
    }
    
    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createDefault(@Nullable String username, @Nullable String password, @Nonnull NetworkIntent intent, @Nonnull NetworkIntent... intents)
    {
        Checks.notNull(intent, "NetworkIntent");
        Checks.noneNull(intents, "NetworkIntent");
        return createDefault(username, password, EnumSet.of(intent, intents));
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createDefault(@Nullable String username, @Nullable String password, @Nonnull Collection<NetworkIntent> intents)
    {
        return create(username, password, intents).applyDefault();
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createDefault(@Nullable String username, @Nullable String password, int intents)
    {
        return new LibraryBuilder(username, password, intents).applyDefault();
    }

    protected LibraryBuilder applyDefault()
    {
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createLight(@Nullable String username, @Nullable String password)
    {
        return new LibraryBuilder(username, password, NetworkIntent.DEFAULT).applyLight();
    }

    protected LibraryBuilder applyLight()
    {
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder create(@Nonnull NetworkIntent intent, @Nonnull NetworkIntent... intents)
    {
        return create(null, null, intent, intents);
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder create(@Nonnull Collection<NetworkIntent> intents)
    {
        return create(null, null, intents);
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder create(@Nullable String username, @Nullable String password, @Nonnull NetworkIntent intent, @Nonnull NetworkIntent... intents)
    {
        return new LibraryBuilder(username, password, NetworkIntent.getRaw(intent, intents)).applyIntents();
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder create(@Nullable String username, @Nullable String password, @Nonnull Collection<NetworkIntent> intents)
    {
        return new LibraryBuilder(username, password, NetworkIntent.getRaw(intents)).applyIntents();
    }

    protected LibraryBuilder applyIntents()
    {
        return this;
    }

    @Nonnull
    public LibraryBuilder setMaxBufferSize(int bufferSize)
    {
        Checks.notNegative(bufferSize, "The buffer size");
        this.maxBufferSize = bufferSize;
        return this;
    }

    /**
     * Changes the internally used EventManager.
     * <br>There are 2 provided Implementations:
     * <ul>
     *     <li>{@link InterfacedEventManager InterfacedEventManager} which uses the Interface
     *     {@link EventListener EventListener} (tip: use the {@link ListenerAdapter ListenerAdapter}).
     *     <br>This is the default EventManager.</li>
     * </ul>
     * <br>You can also create your own EventManager (See {@link IEventManager}).
     *
     * @param  manager
     *         The new {@link IEventManager} to use.
     *
     * @return The {@link LibraryBuilder} instance. Useful for chaining.
     */
    @Nonnull
    public LibraryBuilder setEventManager(@Nullable IEventManager manager)
    {
        this.eventManager = manager;
        return this;
    }

    @Nonnull
    public LibraryBuilder addEventListeners(@Nonnull ListenerAdapter... listeners)
    {
        Checks.noneNull(listeners, "listeners");
        Collections.addAll(this.listeners, listeners);
        return this;
    }

    @Nonnull
    public LibraryBuilder setMaxReconnectDelay(int maxReconnectDelay)
    {
        Checks.check(maxReconnectDelay >= 32, "Max reconnect delay must be 32 seconds or greater. You provided %d.", maxReconnectDelay);

        this.maxReconnectDelay = maxReconnectDelay;
        return this;
    }

    private byte[] initialResponse()
    {
        byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        ByteBuf initialToken = Unpooled.buffer(usernameBytes.length + passwordBytes.length + 2);

        initialToken.writeByte(0);
        initialToken.writeBytes(usernameBytes);
        initialToken.writeByte(0);
        initialToken.writeBytes(passwordBytes);

        return initialToken.array();
    }

    @Nonnull
    public LibraryImpl build()
    {
        byte[] token = initialResponse();
        ThreadingConfig config = new ThreadingConfig();

        SessionController controller = null;

        SessionConfig sessionConfig = new SessionConfig(controller, maxReconnectDelay);

        LibraryImpl library = new LibraryImpl(token, intents, config, sessionConfig, eventManager);

        listeners.forEach(library::addEventListener);
        library.setStatus(Library.Status.INITIALIZED);


        library.getClient().connect();

        return library;
    }
}
