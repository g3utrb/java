
public class ConnectionProperties {
    public String          manager;
    public String          hostname;
    public String          channel;
    public String          username;
    public String          password;
    public String          sendQueue;
    public String          receiveQueue;
    public int             port; 
    public int             minSize;
    public int             maxSize;
    public long            timeout;
    public boolean         compress;
    public boolean         compressOut;
    public ConnectionType  type;

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("Manager:          " + manager + "\n");
        sb.append("Hostname:         " + hostname + "\n");
        sb.append("Channel:          " + channel + "\n");
        sb.append("Username:         " + username + "\n");
        sb.append("SendQueue:        " + sendQueue + "\n");
        sb.append("ReceiveQueue:     " + receiveQueue + "\n");
        sb.append("Port:             " + Integer.toString(port) + "\n");
        sb.append("Min size:         " + Integer.toString(minSize) + "\n");
        sb.append("Max size:         " + Integer.toString(maxSize) + "\n");
        sb.append("Timeout:          " + Long.toString(timeout) + "\n");
        sb.append("Compress:         " + compress + "\n");
        sb.append("CompressOut:      " + compressOut + "\n");
        sb.append("ConnectionType:   " + type + "\n");
        return sb.toString();
    }
}
