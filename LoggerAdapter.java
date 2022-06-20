import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.ConsoleHandler;
import java.util.logging.SimpleFormatter;

public class LoggerAdapter {

    private static LoggerAdapter instance = null;
    private Logger logger = null;

    static LoggerAdapter getInstance() {
        if (null == instance) {
            instance = new LoggerAdapter();
        }
        return instance;
    }

    public void init() throws Exception {
        logger = Logger.getLogger(LoggerAdapter.class.getName());
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private static final String format =
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tN %2$s %3$d %4$s%n";
            @Override
            public synchronized String format(LogRecord rec) {
                Date date = new Date(rec.getMillis());
                String level = rec.getLevel().getLocalizedName();
                long threadId = Thread.currentThread().getId();
                String message = rec.getMessage();
                return String.format(format, date, level, threadId, message);
            }
        });
        logger.addHandler(handler);
    }

    public Logger getLogger() {
        return logger;
    }
}
