import java.sql.Timestamp;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;


/**
 * Class:  Connection
 *
 * Rudimentary connection class with the ability to connect to IBM JMS message
 * queues. Has the ability to send and receive messages. Messages can be plain
 * text or compressed.
 *
 **/
public class Connection implements Comparable<Connection> {

    Logger                   logger;
    MQQueueConnection        connection;
    MQQueueSession           session;
    MQQueueConnectionFactory factory;
    ConnectionProperties     props;
    Timestamp                timestamp;

    /**
     * Constructor
     *
     * Sets member connection properties.
     *
     * @params  connection properties
     */
    public Connection(ConnectionProperties props) {
        this.props = props;
    }

    /**
     * Initialize
     *
     * Must be called on every connection before use:
     * - Establishes mq connection factory.
     * - Establishes and starts mq connection.
     * - Establishes mq session.
     * - Sets logger from singleton.
     */
    public void init() {

        factory = new MQQueueConnectionFactory();
        factory.setQueueManager(props.manager);
        factory.setHostName(props.hostname);
        factory.setPort(props.port);
        factory.setChannel(props.channel);
        factory.setTransportType("dummy");
        factory.setAppName("Webservice");

        if (props.username != null && props.username.isEmpty()) {
            connection = (MQQueueConnection) factory.createQueueConnection();
        }
        else {
            connection = (MQQueueConnection)
                factory.createQueueConnection(props.username, props.password);
        }
        connection.start();
        session = (MQQueueSession) connection.createQueueSession(false, "dummy");
        logger = LoggerAdapter.getInstance().getLogger();
    }

    /**
     * Compress
     *
     * - Establish the deflater.
     * - Deflate message into an output stream.
     *   [could this be an infinite loop].
     * - And serialize the output stream into a bytes message.
     *
     * @params  message to compress
     * @returns compressed bytes message
     */
    private BytesMessage compress(String message) {

        /// establish the deflater
        BytesMessage bytes = session.createBytesMessage();
        byte[] compressed = new byte[50000];
        Deflater deflater = new Deflater();
        deflater.setInput(message.getBytes());
        deflater.finish();

        /// begin the deflation sequence into an output stream
        int count = 0;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        logger.info("Compression sequence starting");
        while (! deflater.finished()) {
            count = deflater.deflate(compressed);
            stream.write(compressed, 0, count);
        }
        deflater.end();
        logger.info("Compression sequence ended");

        /// serialize stream into the bytes message
        bytes.writeBytes(stream.toByteArray());
        return bytes;
    }

    /**
     * Send
     *
     * - Setup the temporary queue and queue sender.
     * - Compress message if necessary.
     * - Otherwise use a plain text message.
     * - Send the message using the sender.
     * - Obtain the correlation id from the bytes message.
     *
     * @params  message to be sent
     * @returns correlation id
     * @throws  JMSException (any others?)
     */
    public String send(String message) throws JMSException {

        /// correlation id is returned, sender must be closed and exception
        /// thrown if necessary - from the finally block
        String correlationId = "";
        MQQueueSender sender = null;
        JMSException  out = null;
        try {

            /// create the temporary queue and sender
            MQQueue queue = session.createQueue(props.sendQueue);
            queue.setTargetClient("dummy");
            sender = session.createSender(queue);

            if (props.compress) {

                /// a compressed send
                BytesMessage bytes = compress(message);
                sender.send(bytes);

                /// to correlate the response message
                correlationId = bytes.getJMSMessageID();
            }
            else {

                /// an uncompressed send, use a text message
                TextMessage textMessage = session.createTextMessage();
                textMessage.setText(message);

                /// dispatch to destination
                sender.send(textMessage);

                /// to correlate the response message
                correlationId = textMessage.getJMSMessageID();
            }
        }
        catch (Exception ex) {

            /// it is not clear what we should do if it is not a JMSException..
            String s = "Failure to send message: " + message;
            Utilities.logException(ex, s);
            if (ex instanceof JMSException) {
                out = (JMSException) ex;
            }
        }
        finally {

            /// there has been some activity on this connection
            timestamp = new Timestamp(System.currentTimeMillis());

            /// if we have a sender make sure to close it
            if (sender != null) {
                try {
                    sender.close();
                }
                catch (Exception ex) {
                    String s = "Failure to close sender. Message: " + message;
                    s += ". Queue type: " + props.type;
                    Utilities.logException(ex, s);
                }
            }
            /// rethrow the exception if there was one
            if (out != null) {
                throw out;
            }
            /// looks like we're ok, return the correlation id
            return correlationId;
        }
    }

    /**
     * Uncompress
     *
     * - Read the message into a bytes [].
     * - Establish the inflater.
     * - Establish the output stream and write buffer.
     * - Enter the inflation loop [we exit when count is zero?]
     * - Convert the output stream to the returned string.
     *
     * @params  message to uncompress
     * @returns uncompressed message
     * @throws  DataFormatException
     */
    private String uncompress(Message message) throws DataFormatException {

        /// read the message into a plain bytes []
        BytesMessage bytesMessage = (BytesMessage) message;
        int length = bytesMessage.getBodyLength();
        byte[] bytes = new byte[length];
        int count = bytesMessage.readBytes(bytes);

        /// setup the inflater with the bytes []
        Inflater inflater = new Inflater();
        inflater.setInput(bytes);

        /// setup the output stream and read buffer
        ByteArrayOutputStream stream = new ByteArrayOutputStream(bytes.length);
        byte[] buffer = new byte[1024];
        logger.info("Decompression sequence starting");

        while (! inflater.finished()) {
            count = inflater.inflate(buffer);

            /// is it possible when everything is finished count is zero
            /// but the inflater still is not finished ?
            if (count == 0) {
                break;
            }
            stream.write(buffer, 0, count);
        }
        /// end the inflater
        inflater.end();
        logger.info("Decompression sequence ended");

        /// produce a string from the output stream
        String result = stream.toString();
        return result;
    }

    /**
     * Receive
     *
     * - Setup the temporary queue and queue sender.
     * - Compress message if necessary.
     * - Otherwise use a plain text message.
     * - Send the message using the sender.
     * - Obtain the correlation id from the bytes message.
     *
     * @params  correlation id of message to receive
     * @returns received message
     * @throws  JMSException, DataFormatException
     */
    public String receive(String correlationId) throws JMSException, DataFormatException {

        /// like send, exception may have to be thrown,
        /// the read content is in the result
        JMSException out = null;
        MQQueueReceiver receiver = null;
        String result = "";
        try {

            /// establish receiver and receive message
            MQQueue queue = session.createQueue(props.receiveQueue);   
            String corrId = "JMSCorrelationID='" + correlationId + "'";
            receiver = session.createReceiver(queue, corrId);
            Message message = receiver.receive(props.timeout);

            if (props.compress && message != null) {
                /// a compressed receive, so uncompress
                result = uncompress(message);
            }
            else if (!props.compress && message != null) {
                /// plain vanilla receive, into text message
                TextMessage textMessage = (TextMessage) message;
                result = textMessage.getText();
            }
            else if (message == null) {
                /// are we throwing here
            }
        }
        catch (Exception ex) {
            String s = "Failure to receive message for correlationId: " + correlationId;
            Utilities.logException(ex, s);
            if (ex instanceof JMSException) {
                out = (JMSException) ex;
            }
        }
        finally {

            /// there has been some activity on this connection
            timestamp = new Timestamp(System.currentTimeMillis());

            /// close the receiver if we have one
            if (receiver != null) {
                try {
                    receiver.close();
                }
                catch (Exception ex) {
                    String s = "Failure to close receiver on correlationId: " + correlationId;
                    s += ". Queue type: " + props.type;
                    Utilities.logException(ex, s);
                }
            }
            /// throw the exception if there was one
            if (out != null) {
                throw out;
            }
            /// otherwise we're ok, return the received message
            return result;
        }
    }

    /**
     * Dispose
     *
     * - Close session and nullify.
     * - Stop connection, close and nullify.
     *   [any other cleanup ???]
     */
    public void dispose() {
        try {
            if (session != null) {
                session.close();
            }
            session = null;
        }
        catch (JMSException ex) {
            String s = "Failed to close session.";
            Utilities.logException(ex, s);
        }
        try {
            if (connection != null) {
                connection.stop();
                connection.close();
            }
            connection = null;
        }
        catch (JMSException ex) {
            String s = "Failed to close connection.";
            Utilities.logException(ex, s);
        }
    }

    /**
     * CompareTo
     *
     * Ensures ordering of connetions by their last update timestamp.
     * This should make oldest connections (with smallest timestamps)
     * appear earlier.
     *
     * @params  other connection being compared against
     * @returns 1 if this greater, -1 if other greater, 0 if equal
     * @throws  none
     */
    public int compareTo(Connection other) {
        if (timestamp == null && other.timestamp == null) {
            return 0;
        }
        else if (timestamp != null && other.timestamp == null) {
            return 1;
        }
        else if (timestamp == null && other.timestamp != null) {
            return -1;
        }
        else {
            return timestamp.compareTo(other.timestamp);
        }
    }
}
