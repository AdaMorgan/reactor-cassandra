/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.adamorgan.api;

import com.github.adamorgan.api.hooks.EventListener;
import com.github.adamorgan.api.hooks.IEventManager;
import com.github.adamorgan.api.hooks.InterfacedEventManager;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.api.utils.ConcurrentSessionController;
import com.github.adamorgan.api.utils.ConfigFlag;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.utils.Checks;
import com.github.adamorgan.internal.utils.LibraryLogger;
import com.github.adamorgan.internal.utils.config.SessionConfig;
import com.github.adamorgan.internal.utils.config.ThreadingConfig;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class LibraryBuilder
{
    public static final Logger LOG = LibraryLogger.getLog(LibraryBuilder.class);

    protected final List<ListenerAdapter> listeners = new LinkedList<>();

    protected final InetSocketAddress address;
    protected final String username;
    protected final String password;

    protected Library.ShardInfo shardInfo;

    protected ThreadFactory threadFactory = null;

    protected ExecutorService eventPool = null;
    protected boolean shutdownEventPool = true;

    protected final EnumSet<ConfigFlag> flags = ConfigFlag.DEFAULT;

    protected IEventManager eventManager = null;
    protected SessionController controller = null;
    protected int maxBufferSize = 1 << 6; // 64 KB
    protected int maxReconnectDelay = 900;
    protected Compression compression = Compression.NONE;
    protected boolean shutdownCallbackPool;

    protected LibraryBuilder(@Nonnull InetSocketAddress address, @Nullable String username, @Nullable String password)
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
    public LibraryBuilder useSharding(int shardId, int shardTotal)
    {
        Checks.notNegative(shardId, "Shard ID");
        Checks.positive(shardTotal, "Shard Total");
        Checks.check(shardId < shardTotal, "The shard ID must be lower than the shardTotal! Shard IDs are 0-based.");
        shardInfo = new Library.ShardInfo(shardId, shardTotal);
        return this;
    }

    @Nonnull
    public LibraryBuilder useKeyspace(String name)
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

    @Nonnull
    public LibraryBuilder setCallbackPool(@Nullable ThreadFactory threadFactory, boolean automaticShutdown)
    {
        this.threadFactory = threadFactory;
        this.shutdownCallbackPool = automaticShutdown;
        return this;
    }

    @Nonnull
    public LibraryBuilder setCallbackPool(@Nullable ThreadFactory threadFactory)
    {
        return this.setCallbackPool(threadFactory, threadFactory == null);
    }

    @Nonnull
    public LibraryBuilder setEventPool(@Nullable ExecutorService executor, boolean automaticShutdown)
    {
        this.eventPool = executor;
        this.shutdownEventPool = automaticShutdown;
        return this;
    }

    @Nonnull
    public LibraryBuilder setEventPool(ExecutorService eventPool)
    {
        return setEventPool(eventPool, eventPool == null);
    }

    @Nonnull
    public LibraryBuilder setEventPassthrough(boolean enable)
    {
        return setFlag(ConfigFlag.EVENT_PASSTHROUGH, enable);
    }

    @Nonnull
    public LibraryBuilder setEnableDebug(boolean enable)
    {
        return setFlag(ConfigFlag.DEBUG, enable);
    }

    @Nonnull
    public LibraryBuilder setEnableShutdownHook(boolean enabled)
    {
        return setFlag(ConfigFlag.SHUTDOWN_HOOK, enabled);
    }

    @Nonnull
    public LibraryBuilder setAutoReconnect(boolean autoReconnect)
    {
        return setFlag(ConfigFlag.AUTO_RECONNECT, autoReconnect);
    }

    @Nonnull
    private LibraryBuilder setFlag(ConfigFlag flag, boolean enable)
    {
        if (enable)
            this.flags.add(flag);
        else
            this.flags.remove(flag);
        return this;
    }

    @Nonnull
    public LibraryBuilder setMaxReconnectDelay(int maxReconnectDelay)
    {
        Checks.check(maxReconnectDelay >= 32, "Max reconnect delay must be 32 seconds or greater. You provided %d.", maxReconnectDelay);
        this.maxReconnectDelay = maxReconnectDelay;
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

        config.setCallbackPool(threadFactory, shutdownCallbackPool);
        config.setEventPool(eventPool, shutdownEventPool);

        SessionController controller = this.controller == null ? new ConcurrentSessionController() : this.controller;
        SessionConfig sessionConfig = new SessionConfig(controller, maxBufferSize, maxReconnectDelay, flags);

        LibraryImpl library = new LibraryImpl(token, address, compression, shardInfo, config, sessionConfig, eventManager);

        listeners.forEach(library::addEventListener);
        library.setStatus(Library.Status.INITIALIZED);

        library.connect();

        return library;
    }
}
