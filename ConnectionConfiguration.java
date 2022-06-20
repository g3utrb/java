import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class ConnectionConfiguration {

    public static ConnectionProperties  readProps;
    public static ConnectionProperties  writeProps;
    public static ConnectionProperties  bulkProps;
    public static long                  evictionPeriod;
    public static int                   overMinimumThreshold;

    public static void init() throws FileNotFoundException, IOException {
        String catalina = System.getenv("SYSTEM");
        String location = catalina + "/conf/mq.properties";

        Properties props = new Properties();
        InputStream stream = new FileInputStream(location);
        props.load(stream);
        loadMQReadProperties(props);
        loadMQWriteProperties(props);
        loadMQBulkProperties(props);

        /// we don't want multiple timers at this point
        String s = props.getProperty("mq.eviction_period", "60000");
        evictionPeriod = Long.parseLong(s);
        s = props.getProperty("mq.over_minimum_threshold", "3");
        overMinimumThreshold = Integer.parseInt(s);
    }

    public static void loadMQReadProperties(Properties props) {
        readProps = new ConnectionProperties();
        readProps.type = ConnectionType.Read;
        readProps.manager = props.getProperty("mq.read.manager");
        readProps.hostname = props.getProperty("mq.read.hostname");
        readProps.channel = props.getProperty("mq.read.channel");
        readProps.port = Integer.parseInt(props.getProperty("mq.read.port"));
        readProps.sendQueue = props.getProperty("mq.read.send_queue");
        readProps.receiveQueue = props.getProperty("mq.read.receive_queue");

        readProps.compress = false;
        String value = props.getProperty("mq.read.compress_message");
        if (value.toLowerCase().equals("true"))
            readProps.compress = true;

        readProps.compressOut = false;
        value = props.getProperty("mq.read.out_compress_message");
        if (value.toLowerCase().equals("true"))
            readProps.compressOut = true;

        readProps.timeout = 0L;
        value = props.getProperty("mq.read.timeout").trim();
        if (! value.isEmpty())
            readProps.timeout = Long.parseLong(value) * 1000L;

        readProps.username = props.getProperty("mq.read.mqusername", "");
        readProps.password = props.getProperty("mq.read.mqpassword", "");
        String minSize = props.getProperty("mq.read_min_pool_size", "10");
        readProps.minSize = Integer.parseInt(minSize);
        String maxSize = props.getProperty("mq.read_max_pool_size", "100");
        readProps.maxSize = Integer.parseInt(maxSize);
    }

    public static void loadMQWriteProperties(Properties props) {
        writeProps = new ConnectionProperties();
        writeProps.type = ConnectionType.Write;
        writeProps.manager = props.getProperty("mq.manager");
        writeProps.hostname = props.getProperty("mq.hostname");
        writeProps.channel = props.getProperty("mq.channel");
        writeProps.port = Integer.parseInt(props.getProperty("mq.port"));
        writeProps.sendQueue = props.getProperty("mq.send_queue");
        writeProps.receiveQueue = props.getProperty("mq.receive_queue");

        writeProps.compress = false;
        String value = props.getProperty("mq.compress_message");
        if (value.toLowerCase().equals("true"))
            writeProps.compress = true;

        writeProps.compressOut = false;
        value = props.getProperty("mq.out_compress_message");
        if (value.toLowerCase().equals("true"))
            writeProps.compressOut = true;

        writeProps.timeout = 0L;
        value = props.getProperty("mq.timeout").trim();
        if (! value.isEmpty())
            writeProps.timeout = Long.parseLong(value) * 1000L;

        writeProps.username = props.getProperty("mq.mqusername", "");
        writeProps.password = props.getProperty("mq.mqpassword", "");
        String minSize = props.getProperty("mq.min_pool_size", "10");
        writeProps.minSize = Integer.parseInt(minSize);
        String maxSize = props.getProperty("mq.max_pool_size", "100");
        writeProps.maxSize = Integer.parseInt(maxSize);
    }

    public static void loadMQBulkProperties(Properties props) {
        bulkProps = new ConnectionProperties();
        bulkProps.type = ConnectionType.Bulk;
        bulkProps.manager = props.getProperty("mq.bulk.manager");
        bulkProps.hostname = props.getProperty("mq.bulk.hostname");
        bulkProps.channel = props.getProperty("mq.bulk.channel");
        bulkProps.port = Integer.parseInt(props.getProperty("mq.bulk.port"));
        bulkProps.sendQueue = props.getProperty("mq.bulk.send_queue");
        bulkProps.receiveQueue = props.getProperty("mq.bulk.receive_queue");

        bulkProps.compress = false;
        String value = props.getProperty("mq.bulk.compress_message");
        if (value.toLowerCase().equals("true"))
            bulkProps.compress = true;

        bulkProps.compressOut = false;
        value = props.getProperty("mq.bulk.out_compress_message");
        if (value.toLowerCase().equals("true"))
            bulkProps.compressOut = true;

        bulkProps.timeout = 0L;
        value = props.getProperty("mq.bulk.timeout").trim();
        if (! value.isEmpty())
            bulkProps.timeout = Long.parseLong(value) * 1000L;

        bulkProps.username = props.getProperty("mq.bulk.mqusername", "");
        bulkProps.password = props.getProperty("mq.bulk.mqpassword", "");
        String minSize = props.getProperty("mq.bulk_min_pool_size", "10");
        bulkProps.minSize = Integer.parseInt(minSize);
        String maxSize = props.getProperty("mq.bulk_max_pool_size", "100");
        bulkProps.maxSize = Integer.parseInt(maxSize);
    }
}
