import java.util.logging.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Connection Pool
 *
 * Contains pools for read, write and bulk requests. Each pool is primed to a
 * minimum size based on its configured properties.  A pool can grow beyond a
 * minimum size to a configured maximum.  Minimal eviction logic exists wherein
 * connections are evicted from the available pool if the available pool has
 * grown beyond the minimum size for a configured interval. The configured
 * interval is reached after successive timer pops of the eviction interval.
 * For instance, if for 3 successive timer pops the available pool has remained
 * remained beyond the minimum size then we can 'assume' that the pool is in a
 * 'quiescent' state since it hasn't been flooded with a barrage of new requests.
 * Connections are ordered based on a last updated timestamp in ascending order.
 * Connections which have been idle for the longest period are at the front of
 * the queue and get evicted first.
 */
public class ConnectionPool {

    /**
     * Locked Pool
     *
     * Locked since access to the pool connections must be thread safe.
     * Also contains the connection configuration properties for the
     * specific pool
     */
    class LockedPool {

        /**
         * Constructor
         *
         * Sets member properties, creates lock and pools.
         *
         * @params  configured connection properties
         */
        LockedPool(ConnectionProperties p) {
            overMinimumCount = 0;
            props = p;
            lock = new ReentrantLock();
            available = new PriorityQueue<Connection>();
            inUse = new PriorityQueue<Connection>();
        }

        /**
         * Serializes pool info into a string
         *
         * @returns stringified contents
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Pool Type: " + props.type + ". ");
            sb.append("Available Connections: " + available.size() + ". ");
            sb.append("InUse Connections: " + inUse.size() + ". ");
            sb.append("Over minimum count: " + overMinimumCount + ".");
            return sb.toString();
        }

        int                        overMinimumCount;
        ConnectionProperties       props;
        ReentrantLock              lock;
        PriorityQueue<Connection>  available;
        PriorityQueue<Connection>  inUse;
    }

    private LockedPool             readPool;
    private LockedPool             writePool;
    private LockedPool             bulkPool;
    private TimerTask              evictionTask;
    private Timer                  evictionTimer;
    private int                    overMinimumThreshold;
    private Logger                 logger;
    private static ConnectionPool  instance = null;

    /**
     * Singleton Accessor
     *
     * @returns singleton instance
     */
    public static ConnectionPool getInstance() {
        if (null == instance) {
            instance = new ConnectionPool();
        }
        return instance;
    }

    /**
     * Initialize
     *
     * - Sets up logger.
     * - Creates the three pools.
     * - Initializes timers
     */
    public void init() {
        logger = LoggerAdapter.getInstance().getLogger();
        readPool = makePool(ConnectionConfiguration.readProps);
        writePool = makePool(ConnectionConfiguration.writeProps);
        bulkPool = makePool(ConnectionConfiguration.bulkProps);
        initTimers();
    }

    /**
     * Initialize Timer
     *
     * Only one timer at this point - the connection eviction timer
     */
    private void initTimers() {
        overMinimumThreshold = ConnectionConfiguration.overMinimumThreshold;
        evictionTask = new TimerTask() {
            public void run() {
                evict();
            }
        };
        evictionTimer = new Timer("EvictionTimer");
        long period = ConnectionConfiguration.evictionPeriod;
        evictionTimer.schedule(evictionTask, period, period);
    }

    /**
     * Make Pool
     *
     * Create a pool and add connections upto the minimum size
     *
     * @params  connection pool properties
     * @returns constructed pool
     */
    private LockedPool makePool(ConnectionProperties props) {
        LockedPool pool = new LockedPool(props);
        for (int i = 0; i < props.minSize; ++i) {
            Connection conn = new Connection(props);
            conn.init();
            pool.available.offer(conn);
        }
        return pool;
    }

    /**
     * Acquire
     *
     * Calls acquireFrom for the supplied pool type.
     *
     * @params  connection type
     * @returns acquired connection
     */
    public Connection acquire(ConnectionType type) throws Exception {
      if (type == ConnectionType.Read) {
        return acquireFrom(readPool);
      }
      else if (type == ConnectionType.Write) {
        return acquireFrom(writePool);
      }
      else {
        return acquireFrom(bulkPool);
      }
    }

    /**
     * Acquire From
     *
     * Thin wrapper to call lockedAcquire; eases the code in the main
     * work function
     *
     * @params  pool being acquired from
     */
    private Connection acquireFrom(LockedPool pool) throws Exception {
        try {
            synchronized (pool.lock) {
                return lockedAcquire(pool);
            }
        } catch (Exception e) {
            String s = "Failed to acquire connection from pool: " + pool;
            Utilities.logException(e, s);
            throw e;
        }
    }

    /**
     * Locked Acquire
     *
     * - Acquires from available pool if not empty.
     * - Else creates a new connection and adds to in-use pool *if* the
     *   in-Use pool size is less than the max size (these are the total
     *   number of connections at thi point)
     * - The pool is full, waits for a connection to arrive in the available
     *   pool and acquires it.
     *
     * @params  pool being acquired from
     */
    private Connection lockedAcquire(LockedPool pool) throws InterruptedException {
        logger.info("Acquiring from -> " + pool);

        /// there is an available connection, use it
        if (! pool.available.isEmpty()) {
            Connection conn = pool.available.poll();
            pool.inUse.offer(conn);
            return conn;
        }
        /// we can still create a new connection and return that
        else if (pool.inUse.size() < pool.props.maxSize) {
            Connection conn = new Connection(pool.props);
            conn.init();
            pool.inUse.offer(conn);
            return conn;
        }
        /// the connection pool is empty, wait for an available connection
        else {
            while (pool.available.isEmpty()) {
                try {
                    logger.info("Pool empty: " + pool);
                    pool.lock.wait();
                }
                catch (InterruptedException e) {
                    String s = "Interrupted waiting on pool: " + pool;
                    Utilities.logException(e, s);
                    throw e;
                }
            }
            Connection conn = pool.available.poll();
            pool.inUse.offer(conn);
            return conn;
        }
    }

    /**
     * Release
     *
     * Calls releaseTo to release connection to appropriate pool.
     *
     * @params  connection being released
     */
    public void release(Connection conn) {
        if (conn.props.type == ConnectionType.Read) {
            releaseTo(conn, readPool);
        }
        else if (conn.props.type == ConnectionType.Write) {
            releaseTo(conn, writePool);
        }
        else if (conn.props.type == ConnectionType.Bulk) {
            releaseTo(conn, bulkPool);
        }
    }

    /**
     * Dispose and Acquire
     *
     * Dispose the supplied connection and acquire a new one.
     *
     * @params  connection being disposed of
     */
    public Connection disposeAcquire(Connection conn) {
        if (conn.props.type == ConnectionType.Read) {
            return lockedDisposeAcquire(conn, readPool);
        }
        else if (conn.props.type == ConnectionType.Write) {
            return lockedDisposeAcquire(conn, writePool);
        }
        else {
            return lockedDisposeAcquire(conn, bulkPool);
        }
    }

    /**
     * Locked Dispose and Acquire
     *
     * - Locks the supplied pool.
     * - Disposes the passed connection.
     * - Removes it from the inUse pool.
     * - Creates and initializes a new connection.
     * - Adds it to the inUse pool.
     *
     * @params   connection being disposed of
     * @params   relevant pool
     * @returns  new connection [on failure the original one]
     */
    public Connection lockedDisposeAcquire(Connection conn, LockedPool pool) {
        Connection incarnated = conn;
        synchronized (pool.lock) {
            try {
                conn.dispose();
                pool.inUse.remove(conn);
                incarnated = lockedAcquire(pool);
                incarnated.timestamp = conn.timestamp;
            }
            catch (Exception ex) {
            }
            return incarnated;
        }
    }

    /**
     * Release To
     *
     * - Removes connection from in-use pool.
     * - Adds connection to available pool.
     * - Notifies possibly waiting thread.
     *
     * @params  connection being released
     * @params  pool being released into
     */
    private void releaseTo(Connection conn, LockedPool pool) {
        synchronized (pool.lock) {
            logger.info("Releasing to   -> " + pool);
            pool.inUse.remove(conn);
            pool.available.offer(conn);
            pool.lock.notify();
        }
    }

    /**
     * Evict
     *
     * Eviction processor, calls evictFrom for each pool.
     */
    private void evict() {
        evictFrom(readPool);
        evictFrom(writePool);
        evictFrom(bulkPool);
    }

    /**
     * Evict From
     *
     * Simple eviction process:
     * - Lock the pool.
     * - Noop if available size is leq the minimum.
     * - We track the number of times the available pool size is over the
     *   minumn, giving it a few tries to get a better feel for quiescence.
     *   If the available pool is over the minimum for some threshold number
     *   of timer pops then we can start evicting old connections from the
     *   pool. We also reset the overMinimumCount to zero for the next cycle.
     * - Otherwise increment overMinimumCount for the next timer pop.
     */
    private void evictFrom(LockedPool pool) {

        logger.info("Timer expired for pool -> " + pool);

        /// acquire lock
        synchronized (pool.lock) {

            /// if available pool size is leq the minimum size
            /// nothing needs to be done
            if (pool.available.size() <= pool.props.minSize) {
                return;
            }
            /// how is the count of over minimum size - if it has exceeded
            /// the threshold then we have to start evicting
            else if (pool.overMinimumCount >= overMinimumThreshold) {
            
                /// iterate over the available pool and evict while the pool
                /// size is greater then the minimum
                for (; pool.available.size() > pool.props.minSize;) {

                    /// get the oldest used connection from the pool
                    Connection conn = pool.available.poll();

                    /// dispose of it cleanly (hopefully)
                    logger.info("Disposing of connection -> " + conn);
                    conn.dispose();
                }
                /// we have shrunk the pool, now reset the count
                pool.overMinimumCount = 0;
            }
            /// otherwise we need some more time to evaluate, increment the
            /// over minimum count for the next timer pop
            else {
                pool.overMinimumCount++;
            }
        }
    }
}
