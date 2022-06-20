public class MQQueueConnectionFactory {

    public void setQueueManager(String manager) {}
    public void setHostName(String hostname) {}
    public void setPort(int port) {}
    public void setChannel(String channel) {}
    public void setTransportType(String transportType) {}
    public void setAppName(String appName) {}

    MQQueueConnection createQueueConnection() {
        return new MQQueueConnection();
    }

    MQQueueConnection createQueueConnection(String user, String password) {
        return new MQQueueConnection();
    }
}
