package com.datastax.api;

import com.datastax.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;
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
        /**{@link Library} is currently attempting to connect it's socket to Apache Cassandra.*/
        CONNECTING_TO_SOCKET(true),
        /**{@link Library} has successfully connected it's socket to Apache Cassandra and is sending authentication*/
        IDENTIFYING_SESSION(true),
        /**{@link Library} is currently attempting to log in.*/
        LOGGING_IN(true),
        /**{@link Library} has sent authentication to Apache Cassandra and is awaiting confirmation*/
        AWAITING_LOGIN_CONFIRMATION(true),
        /**{@link Library} is populating internal objects.
         * This process often takes the longest of all Statuses (besides {@link Status#CONNECTED})*/
        LOADING_SUBSYSTEMS(true),
        /**{@link Library} has finished loading everything, is receiving information from Apache Cassandra and is firing events.*/
        CONNECTED(true),
        /**{@link Library}'s main socket has been disconnected. This <b>DOES NOT</b> mean {@link Library} has shutdown permanently.
         * This is an in-between status. Most likely {@link Status#ATTEMPTING_TO_RECONNECT}
         * or {@link Status#SHUTTING_DOWN}/{@link Status#SHUTDOWN} will soon follow.*/
        DISCONNECTED,
        /** {@link Library} session has been added to {@link com.datastax.api.utils.SessionController SessionController}
         * and is awaiting to be dequeued for reconnecting.*/
        RECONNECT_QUEUED,
        /**When trying to reconnect to Apache Cassandra {@link Library} encountered an issue, most likely related to a lack of internet connection,
         * and is waiting to try reconnecting again.*/
        WAITING_TO_RECONNECT,
        /**{@link Library} has been disconnected from Apache Cassandra and is currently trying to reestablish the connection.*/
        ATTEMPTING_TO_RECONNECT,
        /**{@link Library} has received a shutdown request or has been disconnected from Apache Cassandra and reconnect is disabled, thus,
         * {@link Library} is in the process of shutting down*/
        SHUTTING_DOWN,
        /**{@link Library} has finished shutting down and this instance can no longer be used to communicate with the Apache Cassandra servers.*/
        SHUTDOWN,
        /**While attempting to authenticate, Apache Cassandra reported that the provided authentication information was invalid.*/
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
     * The login token that is currently being used for Apache Cassandra authentication.
     *
     * @return Never-null, an auth bytes token.
     */
    @Nonnull
    byte[] getToken();

    /**
     * Gets the current {@link Status Status} of the {@link com.datastax.api.Library} instance.
     *
     * @return Current {@link Library} status.
     */
    @Nonnull
    Status getStatus();

    /**
     * Adds all provided listeners to the event-listeners that will be used to handle events.
     * This uses the {@link com.datastax.api.hooks.InterfacedEventManager InterfacedEventListener} by default.
     *
     * <p>Note: when using the {@link com.datastax.api.hooks.InterfacedEventManager InterfacedEventListener} (default),
     * given listener <b>must</b> be instanced of {@link com.datastax.api.hooks.EventListener EventListener}!
     *
     * @param  listeners
     *         The listener(s) which will react to events.
     *
     * @throws java.lang.IllegalArgumentException
     *         If either listeners or one of its objects is {@code null}.
     */
    void addEventListener(@Nonnull ListenerAdapter... listeners);

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

    /**
     * This value is the total amount of {@link java.nio.ByteBuffer ByteBuffer} responses that Apache Cassandra has sent.
     * <br>This value resets every time the socket has to perform a full reconnect (not resume).
     *
     * @return Never-negative long containing total response amount.
     */
    long getResponseTotal();

    /**
     * {@link ExecutorService} used to handle {@link com.datastax.api.requests.ObjectAction ObjectAction} callbacks
     * and completions.
     *
     * <br>By default this uses the {@link ForkJoinPool#commonPool() CommonPool} of the runtime.
     *
     * @return The {@link ExecutorService} used for callbacks
     */
    @Nonnull
    ExecutorService getCallbackPool();
}
