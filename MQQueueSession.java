public class MQQueueSession {
    MQQueue createQueue(String name) {
        return new MQQueue();
    }
    MQQueueSender createSender(MQQueue q) {
        return new MQQueueSender();
    }
    MQQueueReceiver createReceiver(MQQueue q, String correlationId) {
        return new MQQueueReceiver();
    }
    BytesMessage createBytesMessage() {
        return new BytesMessage();
    }
    TextMessage createTextMessage() {
        return new TextMessage();
    }
    void close() throws JMSException {
    }
}
