import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

class Utilities {

    public static void logException(Exception ex, String message) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String s = message + " -> " + pw.toString();
            Logger logger = LoggerAdapter.getInstance().getLogger();
            logger.log(Level.SEVERE, s);
        }
        catch (Exception e) {
            ex.printStackTrace();
        }
    }
}
