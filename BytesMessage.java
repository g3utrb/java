public class BytesMessage extends Message {
    public void writeBytes(byte[] bytes) {
    }
    public String getJMSMessageID() {
        return "jms-message-id";
    }
    public int getBodyLength() {
        return 0;
    }
    public int readBytes(byte[] bytes) {
        return 0;
    }
}
