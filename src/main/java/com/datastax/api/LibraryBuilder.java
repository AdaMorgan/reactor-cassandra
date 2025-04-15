package com.datastax.api;

import com.datastax.api.hooks.IEventManager;
import com.datastax.api.hooks.ListenerAdapter;
import com.datastax.internal.LibraryImpl;
import com.datastax.internal.utils.Checks;
import com.datastax.internal.utils.config.ThreadingConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LibraryBuilder
{
    protected final List<ListenerAdapter> listeners = new LinkedList<>();

    private final String username;
    private final String password;
    protected final int intents;

    protected IEventManager eventManager = null;
    protected int maxBufferSize = 2048;

    private LibraryBuilder(String username, String password, int intents)
    {
        this.username = username;
        this.password = password;
        this.intents = 1 | intents;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createDefault(String username, String password, int intents)
    {
        return new LibraryBuilder(username, password, intents).applyDefault();
    }

    protected LibraryBuilder applyDefault()
    {
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder createLight()
    {
        return new LibraryBuilder("cassandra", "cassandra", 0).applyLight();
    }

    protected LibraryBuilder applyLight()
    {
        return this;
    }

    @Nonnull
    @CheckReturnValue
    public static LibraryBuilder create(String username, String password, int intents)
    {
        return new LibraryBuilder(username, password, intents);
    }

    /**
     * Changes the internally used EventManager.
     * <br>There are 2 provided Implementations:
     * <ul>
     *     <li>{@link com.datastax.api.hooks.InterfacedEventManager InterfacedEventManager} which uses the Interface
     *     {@link com.datastax.api.hooks.EventListener EventListener} (tip: use the {@link ListenerAdapter ListenerAdapter}).
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

    public LibraryImpl build()
    {
        byte[] token = initialResponse();
        ThreadingConfig config = new ThreadingConfig();

        LibraryImpl library = new LibraryImpl(token, config, eventManager);

        listeners.forEach(library::addEventListener);
        library.setStatus(Library.Status.INITIALIZED);
        return library;
    }
}
