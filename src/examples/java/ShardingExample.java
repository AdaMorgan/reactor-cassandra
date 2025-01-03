import com.datastax.api.sharding.DefaultObjectManagerBuilder;
import com.datastax.api.sharding.ObjectManager;
import com.datastax.internal.objectaction.ObjectActionImpl;
import com.datastax.internal.request.ObjectRoute;
import org.example.data.DataObject;

import java.net.InetSocketAddress;
import java.util.List;

public class ShardingExample {
    public static void main(String[] args) {
        ObjectManager build = DefaultObjectManagerBuilder.create("cassandra", "cassandra", (username, password) -> {
            return new InetSocketAddress("127.0.0.1", 9042);
        }).build();

        String route = "INSERT INTO demo.accounts(username, bucket, user_id, display_name, flags) VALUES (?, ?, ?, ?, ?)";
        ObjectRoute.CompileRoute compiledRoute = new ObjectRoute(route).compile("tt.tt2998", "1","1062323251105234977", "tt.tt", "0");

        build.getShards().forEach(shard -> {
            new ObjectActionImpl<List<DataObject>>(shard, compiledRoute, ((request, response) -> {
                return response.getObject();
            })).map(List::size).queue(System.out::println, Throwable::printStackTrace);
        });
    }
}
