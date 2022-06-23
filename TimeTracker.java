import java.util.logging.Logger;

public class TimeTracker {
    double  startTime;
    double  stopTime;
    Logger  logger;
    String  stopContext;

    public TimeTracker() {
        startTime = System.currentTimeMillis();
        stopTime = 0;
        logger = LoggerAdapter.getInstance().getLogger();
    }

    public void start(String context) {
        logger.info(context + " has started.");
    }

    public void mark(String context) {
        stopTime = System.currentTimeMillis();
        double elapsed = (stopTime - startTime) / 1000.0;
        StringBuffer sb = new StringBuffer();
        sb.append(context);
        sb.append(" has stopped. Elapsed time: ");
        sb.append(String.format("%.6f", elapsed));
        logger.info(sb.toString());
    }
}
