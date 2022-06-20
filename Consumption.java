import java.util.Vector;
import java.util.logging.*;

public class Consumption {

    public static void main(String[] args) throws Exception {
        LoggerAdapter.getInstance().init();
        Logger logger = LoggerAdapter.getInstance().getLogger();
        ConnectionConfiguration.init();
        ConnectionPool pool = ConnectionPool.getInstance();
        pool.init();
        Vector<Thread> threads = new Vector<Thread>();
        for (int i = 0; i < 20; ++i) {
            Thread thread = new Thread(() -> {
                for (;;) {
                    try {
                        Connection conn = pool.acquire(ConnectionType.Read);
                        Thread.sleep(5000);
                        pool.release(conn);
                   } catch (Exception ex) {
                       ex.printStackTrace();
                   }
                }
            });
            threads.add(thread);
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            try {
                t.join();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
