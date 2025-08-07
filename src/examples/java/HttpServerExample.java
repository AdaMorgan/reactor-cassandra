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
import com.github.adamorgan.api.hooks.ListenerAdapter;
import com.github.adamorgan.api.requests.Response;
import com.github.adamorgan.api.utils.Compression;
import com.github.adamorgan.internal.LibraryImpl;
import com.github.adamorgan.internal.requests.Requester;
import com.github.adamorgan.internal.requests.action.ObjectActionImpl;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class HttpServerExample extends ListenerAdapter
{
    public static final String TEST_QUERY_PREPARED = "SELECT * FROM system.local WHERE bootstrapped = 'COMPLETED' ALLOW FILTERING";

    public static void main(String[] args)
    {
        InetSocketAddress address = InetSocketAddress.createUnresolved("127.0.0.1", 9042);

        LibraryImpl api = LibraryBuilder.createLight(address, "cassandra", "cassandra")
                .addEventListeners(new HttpServerExample())
                .setEnableDebug(false)
                .build();

        request(api);
    }

    public static void request(LibraryImpl api)
    {
        long startTime = System.currentTimeMillis();
        final int count = 1000;

        Map<Integer, Response> responseMap = new HashMap<>();

        for (int i = 0; i < count; i++)
        {
            int finalI = i;

            api.sendRequest(TEST_QUERY_PREPARED).queue(response -> {
                responseMap.put(finalI, response);
                if (responseMap.size() == count)
                {
                    long duration = System.currentTimeMillis() - startTime;
                    System.out.println("Total time: " + duration + " ms");
                    System.out.println("RPS: " + Math.round(count * 1000.0 / duration));
                }
            });
        }
    }
}
