package org.apache.tools.ant.listener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
public class AnsiColorLogger extends DefaultLogger {
    private static final int ATTR_DIM = 2;
    private static final int FG_RED = 31;
    private static final int FG_GREEN = 32;
    private static final int FG_BLUE = 34;
    private static final int FG_MAGENTA = 35;
    private static final int FG_CYAN = 36;
    private static final String PREFIX = "\u001b[";
    private static final String SUFFIX = "m";
    private static final char SEPARATOR = ';';
    private static final String END_COLOR = PREFIX + SUFFIX;
    private String errColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_RED + SUFFIX;
    private String warnColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_MAGENTA + SUFFIX;
    private String infoColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_CYAN + SUFFIX;
    private String verboseColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_GREEN + SUFFIX;
    private String debugColor
        = PREFIX + ATTR_DIM + SEPARATOR + FG_BLUE + SUFFIX;
    private boolean colorsSet = false;
    private void setColors() {
        String userColorFile = System.getProperty("ant.logger.defaults");
        String systemColorFile =
            "/org/apache/tools/ant/listener/defaults.properties";
        InputStream in = null;
        try {
            Properties prop = new Properties();
            if (userColorFile != null) {
                in = new FileInputStream(userColorFile);
            } else {
                in = getClass().getResourceAsStream(systemColorFile);
            }
            if (in != null) {
                prop.load(in);
            }
            String errC = prop.getProperty("AnsiColorLogger.ERROR_COLOR");
            String warn = prop.getProperty("AnsiColorLogger.WARNING_COLOR");
            String info = prop.getProperty("AnsiColorLogger.INFO_COLOR");
            String verbose = prop.getProperty("AnsiColorLogger.VERBOSE_COLOR");
            String debug = prop.getProperty("AnsiColorLogger.DEBUG_COLOR");
            if (errC != null) {
                errColor = PREFIX + errC + SUFFIX;
            }
            if (warn != null) {
                warnColor = PREFIX + warn + SUFFIX;
            }
            if (info != null) {
                infoColor = PREFIX + info + SUFFIX;
            }
            if (verbose != null) {
                verboseColor = PREFIX + verbose + SUFFIX;
            }
            if (debug != null) {
                debugColor = PREFIX + debug + SUFFIX;
            }
        } catch (IOException ioe) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }
    protected void printMessage(final String message,
                                      final PrintStream stream,
                                      final int priority) {
        if (message != null && stream != null) {
            if (!colorsSet) {
                setColors();
                colorsSet = true;
            }
            final StringBuffer msg = new StringBuffer(message);
            switch (priority) {
                case Project.MSG_ERR:
                    msg.insert(0, errColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_WARN:
                    msg.insert(0, warnColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_INFO:
                    msg.insert(0, infoColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_VERBOSE:
                    msg.insert(0, verboseColor);
                    msg.append(END_COLOR);
                    break;
                case Project.MSG_DEBUG:
                default:
                    msg.insert(0, debugColor);
                    msg.append(END_COLOR);
                    break;
            }
            final String strmessage = msg.toString();
            stream.println(strmessage);
        }
    }
}
