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

import com.github.adamorgan.api.LibraryBuilder;
import com.github.adamorgan.api.events.ExceptionEvent;
import com.github.adamorgan.api.events.StatusChangeEvent;
import com.github.adamorgan.api.events.session.ReadyEvent;
import com.github.adamorgan.api.events.session.ShutdownEvent;
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public final class SessionLoggerExample extends ListenerAdapter
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM system.clients WHERE connection_stage = :stage ALLOW FILTERING";
    public static final String TEST_QUERY = "SELECT hostname FROM system.clients";

    public static void main(String[] args)
    {
        InetSocketAddress address = InetSocketAddress.createUnresolved("127.0.0.1", 9042);

        LibraryImpl api = LibraryBuilder.createLight(address, "cassandra", "cassandra")
                .addEventListeners(new SessionLoggerExample())
                .setCompression(Compression.SNAPPY)
                .setEnableDebug(false)
                .build();
    }

    @Override
    public void onStatusChange(@Nonnull StatusChangeEvent event)
    {
        LibraryImpl.LOG.info("{} -> {}", event.getOldStatus(), event.getNewStatus());
    }

    @Override
    public void onException(@Nonnull ExceptionEvent event)
    {
        LibraryImpl.LOG.error(event.getMessage());
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event)
    {
        //LibraryImpl.LOG.info("Finished Loading!");
    }

    @Override
    public void onShutdown(@Nonnull ShutdownEvent event)
    {
        LibraryImpl.LOG.info("Shutting down...");
    }
}
