public class MQQueueConnection {
    public void start() {}
    public MQQueueSession createQueueSession(boolean p1, String p2) {
        return new MQQueueSession();
    }
    public void stop() throws JMSException {
    }
    public void close() throws JMSException {
    }
}
