import java.util.PriorityQueue;
import java.sql.Timestamp;

public class Timestamps {

    public PriorityQueue<Connection>  connections;

    public static void main(String[] args) throws Exception {
        LoggerAdapter.getInstance().init();
        Timestamps t = new Timestamps();
        t.connections = new PriorityQueue<>();

        ConnectionProperties props = new ConnectionProperties();
        props.type = ConnectionType.Read;
        Connection c = new Connection(props);
        c.init();
        t.connections.offer(c);

        props = new ConnectionProperties();
        props.type = ConnectionType.Write;
        c = new Connection(props);
        c.init();
        c.timestamp = new Timestamp(System.currentTimeMillis());
        t.connections.offer(c);
        for (Connection conn : t.connections) {
            System.out.println(conn.timestamp);
        }
    }
}
