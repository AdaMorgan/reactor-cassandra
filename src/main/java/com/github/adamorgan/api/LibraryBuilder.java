package com.github.adamorgan.api;

import com.github.adamorgan.api.hooks.EventListener;
import com.github.adamorgan.api.hooks.IEventManager;
import com.github.adamorgan.api.hooks.InterfacedEventManager;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.ConcurrentSessionController;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.config.SessionConfig;
import com.github.adamorgan.internal.utils.config.ThreadingConfig;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LibraryBuilder
{
    protected final List<ListenerAdapter> listeners = new LinkedList<>();

    protected final InetSocketAddress address;
    protected final String username;
    protected final String password;

    protected IEventManager eventManager = null;
    protected SessionController controller = null;
    protected int maxBufferSize = 5000;
    protected int maxReconnectDelay = 900;
    protected Compression compression;

    private LibraryBuilder(@Nonnull InetSocketAddress address, @Nullable String username, @Nullable String password)
    {
        this.address = address;
        this.username = username == null ? StringUtils.EMPTY : username;
        this.password = password == null ? StringUtils.EMPTY : password;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder create(@Nonnull InetSocketAddress address, @Nullable String username, @Nullable String password)
    {
        Checks.notNull(address, "Address");
        return new LibraryBuilder(address, username, password);
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder create(@Nonnull InetSocketAddress address)
    {
        return create(address, null, null);
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createDefault(@Nonnull InetSocketAddress address, @Nullable String username, @Nullable String password)
    {
        Checks.notNull(address, "Address");
        return new LibraryBuilder(address, username, password).applyDefault();
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createDefault(@Nonnull InetSocketAddress address)
    {
        return createDefault(address, null, null);
    }

    protected LibraryBuilder applyDefault()
    {
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createLight(@Nonnull InetSocketAddress address, @Nullable String username, @Nullable String password)
    {
        Checks.notNull(address, "Address");
        return new LibraryBuilder(address, username, password).applyLight();
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createLight(@Nonnull InetSocketAddress address)
    {
        return createLight(address, null, null);
    }

    protected LibraryBuilder applyLight()
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

    @Nonnull
    public LibraryBuilder setCompression(@Nonnull Compression compression)
    {
        Checks.notNull(compression, "Compression");
        this.compression = compression;
        return this;
    }

    @Nonnull
    public LibraryBuilder setSessionController(@Nullable SessionController controller)
    {
        this.controller = controller;
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

    private byte[] verifyToken()
    {
        byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        return Unpooled.buffer(usernameBytes.length + passwordBytes.length + 2)
                .writeByte(0)
                .writeBytes(usernameBytes)
                .writeByte(0)
                .writeBytes(passwordBytes)
                .array();
    }

    @Nonnull
    public LibraryImpl build()
    {
        byte[] token = verifyToken();
        ThreadingConfig config = new ThreadingConfig();

        SessionController controller = this.controller == null ? new ConcurrentSessionController() : this.controller;
        SessionConfig sessionConfig = new SessionConfig(controller, maxBufferSize, maxReconnectDelay);

        LibraryImpl library = new LibraryImpl(token, address, compression, config, sessionConfig, eventManager);

        listeners.forEach(library::addEventListener);
        library.setStatus(Library.Status.INITIALIZED);

        library.getClient().connect();

        return library;
    }
}
