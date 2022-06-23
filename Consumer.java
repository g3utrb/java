import java.util.Vector;
import java.util.logging.*;

public class Consumer {

    public static class ConsumerTask extends Thread {

        private int    id;
        private int    count;
        private Logger logger;

        public ConsumerTask(int id) {
            this.id = id;
            this.count = 0;
            this.logger = LoggerAdapter.getInstance().getLogger();
        }

        public void run() {
            ConnectionPool pool = ConnectionPool.getInstance();
            for (;;) {
                Connection conn = null;
                try {
                    conn = pool.acquire(ConnectionType.Read);
                    Dispatcher.Result res = Dispatcher.dispatch("foo", conn, true);
                    conn = res.conn;
                    Thread.sleep(2000);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                finally {
                    pool.release(conn);
                    if (++count >= 3 && id < 20) {
                        logger.info("Stopping thread: " + this);
                        break;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        LoggerAdapter.getInstance().init();
        Logger logger = LoggerAdapter.getInstance().getLogger();
        ConnectionConfiguration.init();
        ConnectionPool pool = ConnectionPool.getInstance();
        pool.init();
        Vector<Thread> threads = new Vector<Thread>();
        for (int i = 0; i < 30; ++i) {
            Thread thread = new Consumer.ConsumerTask(i);
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
