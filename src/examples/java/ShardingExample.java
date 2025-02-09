import com.datastax.api.sharding.DefaultObjectManagerBuilder;
import com.datastax.api.sharding.ObjectManager;
import com.datastax.internal.objectaction.ObjectActionImpl;
import com.datastax.internal.request.ObjectRoute;
import org.example.data.DataObject;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ShardingExample {
    public static void main(String[] args)
    {
        ObjectManager build = DefaultObjectManagerBuilder.create("cassandra", "cassandra", (username, password) -> {
            return new InetSocketAddress("127.0.0.1", 9042);
        }).build();

        String route = "SELECT * FROM system.clients";
        ObjectRoute.CompileRoute compiledRoute = new ObjectRoute(route).compile("tt.tt2998", "1","1062323251105234977", "tt.tt", "0");

        build.getShards().forEach(shard -> {
            CompletableFuture<Integer> submit = new ObjectActionImpl<List<DataObject>>(shard, compiledRoute, ((request, response) -> response.getObject()))
                    .map(List::size)
                    .filter(c -> c == 2)
                    .submit();

            try
            {
                System.out.println(submit.get() == null);
            } catch (InterruptedException | ExecutionException e)
            {
                throw new RuntimeException(e);
            }
        });
    }
}
