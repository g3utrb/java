import java.util.logging.Logger;

/**
 * Dispatcher
 *
 * Named dispatcher but its main purpose is to send or receive a message within
 * a given retry threshold since the caller should not be exposed to this snafu.
 */
public class Dispatcher {

    /**
     * Should be a java Pair but earlier compilers did not have this.
     */
    public static class Result {

        Result(String result, Connection conn) {
            this.result = result;
            this.conn = conn;
        }

        String result;
        Connection conn;
    };

    public static int retries = 3;

    /**
     * Dispatch
     *
     * For retries count, attempts to dispatch supplied message. If we hit
     * a JMS exception, dispose of the old connectiona dnd create a new one.
     * What should we do if we fail even after all retries - throw?
     *
     * @param   content - message to send or id of message to receive
     * @param   send    - true if sending, false if receiving
     * @return  a pair of the result and the (possibly new) connection
     */
    public static Dispatcher.Result
    dispatch(String content, Connection conn, boolean send) {

        Logger logger = LoggerAdapter.getInstance().getLogger();
        String response = "";
        boolean dispatched = false;
        for (int i = 0; i < retries; ++i) {
            try {
                if (send) {
                    response = conn.send(content);
                }
                else {
                    response = conn.receive(content);
                }
            }
            catch (JMSException je) {
                conn = ConnectionPool.getInstance().disposeAcquire(conn);
            }
            catch (Exception ex) {
                break;
            }
        }
        if (! dispatched) {
            /// throw
        }
        return new Dispatcher.Result(response, conn);
    }
}
