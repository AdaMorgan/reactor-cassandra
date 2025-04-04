import com.datastax.internal.utils.concurrent.CountingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.BiFunction;

public class StreamManager
{
    protected final Queue<Integer> queue = new ConcurrentLinkedQueue<>();
    protected final ExecutorService executor;

    public StreamManager(int totalStreams)
    {
        this.executor = Executors.newScheduledThreadPool(totalStreams, new CountingThreadFactory(() -> "Stream", "Worker"));

        for (int i = 0; i < totalStreams; i++)
        {
            queue.add(i);
        }
    }

    public <T> void execute(@Nonnull Object api, @Nonnull BiFunction<Object, Integer, T> command)
    {
        CompletableFuture<T> future = new CompletableFuture<>();

        Integer streamId = queue.poll();
        if (streamId == null)
        {
            future.completeExceptionally(new IllegalStateException("No available streams"));
            return;
        }

        executor.execute(() ->
        {
            try
            {
                T result = command.apply(api, streamId);
                future.complete(result);
            }
            catch (Exception e)
            {
                future.completeExceptionally(e);
            }
            finally
            {
                queue.add(streamId);
            }
        });

    }

    public void shutdown()
    {
        executor.shutdown();
    }

    public static final Logger LOG = LoggerFactory.getLogger(StreamManager.class);

    public static void main(String[] args) throws InterruptedException
    {
        StreamManager manager = new StreamManager(10);
        Object api = new Object();

        for (int i = 0; i < 6; i++)
        {
            manager.execute(api, (a, streamId) ->
            {
                LOG.info("[{}] Processing Stream ID: {}\n", Thread.currentThread()
                        .getName(), streamId);
                return null;
            });
        }

        TimeUnit.SECONDS.sleep(3);

        manager.shutdown();
    }
}