package com.github.adamorgan.api;

import com.github.adamorgan.api.hooks.EventListener;
import com.github.adamorgan.api.hooks.InterfacedEventManager;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.requests.NetworkIntent;
import com.github.adamorgan.api.requests.ObjectAction;
import com.github.adamorgan.api.requests.objectaction.ObjectCreateAction;
import com.github.adamorgan.api.utils.SessionController;
import com.github.adamorgan.api.utils.cache.CacheView;
import com.github.adamorgan.internal.requests.action.ObjectCreateActionImpl;
import com.github.adamorgan.internal.utils.Checks;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

public interface Library
{
    /**
     * Represents the connection status of {@link Library} and its Main Socket.
     */
    enum Status
    {
        /**{@link Library} is currently setting up supporting systems like the AudioSystem.*/
        INITIALIZING(true),
        /**{@link Library} has finished setting up supporting systems and is ready to log in.*/
        INITIALIZED(true),
        /**{@link Library} is currently attempting to connect it's socket to CQL Binary Protocol.*/
        CONNECTING_TO_SOCKET(true),
        /**{@link Library} has successfully connected it's socket to CQL Binary Protocol and is sending authentication*/
        IDENTIFYING_SESSION(true),
        /**{@link Library} is currently attempting to log in.*/
        LOGGING_IN(true),
        /**{@link Library} has sent authentication to CQL Binary Protocol and is awaiting confirmation*/
        AWAITING_LOGIN_CONFIRMATION(true),
        /**{@link Library} has finished loading everything, is receiving information from CQL Binary Protocol and is firing events.*/
        CONNECTED(true),
        /**{@link Library}'s main socket has been disconnected. This <b>DOES NOT</b> mean {@link Library} has shutdown permanently.
         * This is an in-between status. Most likely {@link Status#ATTEMPTING_TO_RECONNECT}
         * or {@link Status#SHUTTING_DOWN}/{@link Status#SHUTDOWN} will soon follow.*/
        DISCONNECTED,
        /** {@link Library} session has been added to {@link SessionController SessionController}
         * and is awaiting to be dequeued for reconnecting.*/
        RECONNECT_QUEUED,
        /**When trying to reconnect to CQL Binary Protocol {@link Library} encountered an issue, most likely related to a lack of internet connection,
         * and is waiting to try reconnecting again.*/
        WAITING_TO_RECONNECT,
        /**{@link Library} has been disconnected from CQL Binary Protocol and is currently trying to reestablish the connection.*/
        ATTEMPTING_TO_RECONNECT,
        /**{@link Library} has received a shutdown request or has been disconnected from CQL Binary Protocol and reconnect is disabled, thus,
         * {@link Library} is in the process of shutting down*/
        SHUTTING_DOWN,
        /**{@link Library} has finished shutting down and this instance can no longer be used to communicate with the CQL Binary Protocol.*/
        SHUTDOWN,
        /**While attempting to authenticate, CQL Binary Protocol reported that the provided authentication information was invalid.*/
        FAILED_TO_LOGIN;

        private final boolean isInit;

        Status(boolean isInit)
        {
            this.isInit = isInit;
        }

        Status()
        {
            this.isInit = false;
        }

        public boolean isInit()
        {
            return isInit;
        }
    }

    /**
     * The login token that is currently being used for CQL Binary Protocol authentication.
     *
     * @return Never-null, an auth bytes token.
     */
    @Nonnull
    byte[] getToken();

    /**
     * Gets the current {@link Status Status} of the {@link Library} instance.
     *
     * @return Current {@link Library} status.
     */
    @Nonnull
    Status getStatus();

    /**
     * The {@link NetworkIntent NetworkIntents} for this {@value LibraryInfo#PROJECT_NAME } session.
     *
     * @return {@link EnumSet} of active Session intents
     */
    @Nonnull
    EnumSet<NetworkIntent> getNetworkIntents();

    /**
     * Adds all provided listeners to the event-listeners that will be used to handle events.
     * This uses the {@link InterfacedEventManager InterfacedEventListener} by default.
     *
     * <p>Note: when using the {@link InterfacedEventManager InterfacedEventListener} (default),
     * given listener <b>must</b> be instanced of {@link EventListener EventListener}!
     *
     * @param  listeners
     *         The listener(s) which will react to events.
     *
     * @throws java.lang.IllegalArgumentException
     *         If either listeners or one of its objects is {@code null}.
     */
    void addEventListener(@Nonnull ListenerAdapter... listeners);

    @Nonnull
    CacheView<ObjectCreateAction> getObjectCache();

    /**
     * Removes all provided listeners from the event-listeners and no longer uses them to handle events.
     *
     * @param  listeners
     *         The listener(s) to be removed.
     *
     * @throws java.lang.IllegalArgumentException
     *         If either listeners or one of it's objects is {@code null}.
     */
    void removeEventListener(@Nonnull ListenerAdapter... listeners);

    /**
     * Immutable List of Objects that have been registered as EventListeners.
     *
     * @return List of currently registered Objects acting as EventListeners.
     */
    @Nonnull
    List<? extends ListenerAdapter> getRegisteredListeners();

    @Nonnull
    @CheckReturnValue
    default ObjectCreateAction sendRequest(@Nonnull CharSequence text, @Nonnull Object... args)
    {
        Checks.notNull(text, "Content");
        return new ObjectCreateActionImpl(this).setContent(text.toString(), args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
    }

    @Nonnull
    @CheckReturnValue
    default ObjectCreateAction sendRequest(@Nonnull CharSequence text, @Nullable ObjectCreateAction.Consistency consistency, @Nonnull Object... args)
    {
        Checks.notNull(text, "Content");
        return new ObjectCreateActionImpl(this, consistency).setContent(text.toString(), args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
    }

    @Nonnull
    @CheckReturnValue
    default <R> ObjectCreateAction sendRequest(@Nonnull CharSequence text, @Nonnull Collection<? super R> args)
    {
        Checks.notNull(text, "Content");
        return new ObjectCreateActionImpl(this).setContent(text.toString(), args);
    }

    @Nonnull
    @CheckReturnValue
    default <R> ObjectCreateAction sendRequest(@Nonnull CharSequence text, @Nullable ObjectCreateAction.Consistency consistency, @Nonnull Collection<? super R> args)
    {
        Checks.notNull(text, "Content");
        return new ObjectCreateActionImpl(this, consistency).setContent(text.toString(), args);
    }

    @Nonnull
    @CheckReturnValue
    default <R> ObjectCreateAction sendRequest(@Nonnull CharSequence text, @Nonnull Map<String, ? super R> args)
    {
        Checks.notNull(text, "Content");
        return new ObjectCreateActionImpl(this).setContent(text.toString(), args);
    }

    @Nonnull
    @CheckReturnValue
    default <R> ObjectCreateAction sendRequest(@Nonnull CharSequence text, @Nullable ObjectCreateAction.Consistency consistency, @Nonnull Map<String, ? super R> args)
    {
        Checks.notNull(text, "Content");
        return new ObjectCreateActionImpl(this, consistency).setContent(text.toString(), args);
    }

    /**
     * This value is the maximum amount of time, in seconds, that {@value LibraryInfo#PROJECT_NAME } will wait between reconnect attempts.
     * <br>Can be set using {@link LibraryBuilder#setMaxReconnectDelay(int) LibraryBuilder.setMaxReconnectDelay(int)}.
     *
     * @return The maximum amount of time {@value LibraryInfo#PROJECT_NAME } will wait between reconnect attempts in seconds.
     */
    int getMaxReconnectDelay();

    /**
     * This value is the total amount of {@link java.nio.ByteBuffer ByteBuffer} responses that CQL Binary Protocol has sent.
     * <br>This value resets every time the socket has to perform a full reconnect (not resume).
     *
     * @return Never-negative long containing total response amount.
     */
    long getResponseTotal();

    /**
     * {@link ExecutorService} used to handle {@link ObjectAction ObjectAction} callbacks
     * and completions.
     *
     * <br>By default this uses the {@link ForkJoinPool#commonPool() CommonPool} of the runtime.
     *
     * @return The {@link ExecutorService} used for callbacks
     */
    @Nonnull
    ExecutorService getCallbackPool();
}
