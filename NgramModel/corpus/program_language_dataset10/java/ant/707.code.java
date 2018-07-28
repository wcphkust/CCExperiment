package org.apache.tools.ant.types.resources.selectors;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Locale;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.TimeComparison;
import org.apache.tools.ant.util.FileUtils;
public class Date implements ResourceSelector {
    private static final String MILLIS_OR_DATETIME
        = "Either the millis or the datetime attribute must be set.";
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private Long millis = null;
    private String dateTime = null;
    private String pattern = null;
    private TimeComparison when = TimeComparison.EQUAL;
    private long granularity = FILE_UTILS.getFileTimestampGranularity();
    public synchronized void setMillis(long m) {
        millis = new Long(m);
    }
    public synchronized long getMillis() {
        return millis == null ? -1L : millis.longValue();
    }
    public synchronized void setDateTime(String s) {
        dateTime = s;
        millis = null;
    }
    public synchronized String getDatetime() {
        return dateTime;
    }
    public synchronized void setGranularity(long g) {
        granularity = g;
    }
    public synchronized long getGranularity() {
        return granularity;
    }
    public synchronized void setPattern(String p) {
        pattern = p;
    }
    public synchronized String getPattern() {
        return pattern;
    }
    public synchronized void setWhen(TimeComparison c) {
        when = c;
    }
    public synchronized TimeComparison getWhen() {
        return when;
    }
    public synchronized boolean isSelected(Resource r) {
        if (dateTime == null && millis == null) {
            throw new BuildException(MILLIS_OR_DATETIME);
        }
        if (millis == null) {
            DateFormat df = ((pattern == null)
                ? DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT, Locale.US)
                : new SimpleDateFormat(pattern));
            try {
                long m = df.parse(dateTime).getTime();
                if (m < 0) {
                    throw new BuildException("Date of " + dateTime
                        + " results in negative milliseconds value"
                        + " relative to epoch (January 1, 1970, 00:00:00 GMT).");
                }
                setMillis(m);
            } catch (ParseException pe) {
                throw new BuildException("Date of " + dateTime
                        + " Cannot be parsed correctly. It should be in"
                        + (pattern == null
                        ? " MM/DD/YYYY HH:MM AM_PM" : pattern) + " format.");
            }
        }
        return when.evaluate(r.getLastModified(), millis.longValue(), granularity);
    }
}