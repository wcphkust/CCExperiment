package org.apache.tools.ant.taskdefs.optional;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.LayoutPreservingProperties;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;
public class PropertyFile extends Task {
    private String              comment;
    private Properties          properties;
    private File                propertyfile;
    private boolean             useJDKProperties;
    private Vector entries = new Vector();
    public void execute() throws BuildException {
        checkParameters();
        readFile();
        executeOperation();
        writeFile();
    }
    public Entry createEntry() {
        Entry e = new Entry();
        entries.addElement(e);
        return e;
    }
    private void executeOperation() throws BuildException {
        for (Enumeration e = entries.elements(); e.hasMoreElements();) {
            Entry entry = (Entry) e.nextElement();
            entry.executeOn(properties);
        }
    }
    private void readFile() throws BuildException {
        if (useJDKProperties) {
            properties = new Properties();
        } else {
            properties = new LayoutPreservingProperties();
        }
        try {
            if (propertyfile.exists()) {
                log("Updating property file: "
                    + propertyfile.getAbsolutePath());
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(propertyfile);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    properties.load(bis);
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            } else {
                log("Creating new property file: "
                    + propertyfile.getAbsolutePath());
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(propertyfile.getAbsolutePath());
                    out.flush();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        } catch (IOException ioe) {
            throw new BuildException(ioe.toString());
        }
    }
    private void checkParameters() throws BuildException {
        if (!checkParam(propertyfile)) {
            throw new BuildException("file token must not be null.",
                                     getLocation());
        }
    }
    public void setFile(File file) {
        propertyfile = file;
    }
    public void setComment(String hdr) {
        comment = hdr;
    }
    public void setJDKProperties(boolean val) {
        useJDKProperties = val;
    }
    private void writeFile() throws BuildException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            properties.store(baos, comment);
        } catch (IOException x) { 
            throw new BuildException(x, getLocation());
        }
        try {
            OutputStream os = new FileOutputStream(propertyfile);
            try {
                try {
                    os.write(baos.toByteArray());
                } finally {
                    os.close();
                }
            } catch (IOException x) { 
                FileUtils.getFileUtils().tryHardToDelete(propertyfile);
                throw x;
            }
        } catch (IOException x) { 
            throw new BuildException(x, getLocation());
        }
    }
    private boolean checkParam(File param) {
        return !(param == null);
    }
    public static class Entry {
        private static final int DEFAULT_INT_VALUE = 0;
        private static final String DEFAULT_DATE_VALUE = "now";
        private static final String DEFAULT_STRING_VALUE = "";
        private String              key = null;
        private int                 type = Type.STRING_TYPE;
        private int                 operation = Operation.EQUALS_OPER;
        private String              value = null;
        private String              defaultValue = null;
        private String              newValue = null;
        private String              pattern = null;
        private int                 field = Calendar.DATE;
        public void setKey(String value) {
            this.key = value;
        }
        public void setValue(String value) {
            this.value = value;
        }
        public void setOperation(Operation value) {
            this.operation = Operation.toOperation(value.getValue());
        }
        public void setType(Type value) {
            this.type = Type.toType(value.getValue());
        }
        public void setDefault(String value) {
            this.defaultValue = value;
        }
        public void setPattern(String value) {
            this.pattern = value;
        }
        public void setUnit(PropertyFile.Unit unit) {
            field = unit.getCalendarField();
        }
        protected void executeOn(Properties props) throws BuildException {
            checkParameters();
            if (operation == Operation.DELETE_OPER) {
                props.remove(key);
                return;
            }
            String oldValue = (String) props.get(key);
            try {
                if (type == Type.INTEGER_TYPE) {
                    executeInteger(oldValue);
                } else if (type == Type.DATE_TYPE) {
                    executeDate(oldValue);
                } else if (type == Type.STRING_TYPE) {
                    executeString(oldValue);
                } else {
                    throw new BuildException("Unknown operation type: "
                                             + type);
                }
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }
            if (newValue == null) {
                newValue = "";
            }
            props.put(key, newValue);
        }
        private void executeDate(String oldValue) throws BuildException {
            Calendar currentValue = Calendar.getInstance();
            if (pattern == null) {
                pattern = "yyyy/MM/dd HH:mm";
            }
            DateFormat fmt = new SimpleDateFormat(pattern);
            String currentStringValue = getCurrentValue(oldValue);
            if (currentStringValue == null) {
                currentStringValue = DEFAULT_DATE_VALUE;
            }
            if ("now".equals(currentStringValue)) {
                currentValue.setTime(new Date());
            } else {
                try {
                    currentValue.setTime(fmt.parse(currentStringValue));
                } catch (ParseException pe)  {
                }
            }
            if (operation != Operation.EQUALS_OPER) {
                int offset = 0;
                try {
                    offset = Integer.parseInt(value);
                    if (operation == Operation.DECREMENT_OPER) {
                        offset = -1 * offset;
                    }
                } catch (NumberFormatException e) {
                    throw new BuildException("Value not an integer on " + key);
                }
                currentValue.add(field, offset);
            }
            newValue = fmt.format(currentValue.getTime());
        }
        private void executeInteger(String oldValue) throws BuildException {
            int currentValue = DEFAULT_INT_VALUE;
            int newV  = DEFAULT_INT_VALUE;
            DecimalFormat fmt = (pattern != null) ? new DecimalFormat(pattern)
                : new DecimalFormat();
            try {
                String curval = getCurrentValue(oldValue);
                if (curval != null) {
                    currentValue = fmt.parse(curval).intValue();
                } else {
                    currentValue = 0;
                }
            } catch (NumberFormatException nfe) {
            } catch (ParseException pe)  {
            }
            if (operation == Operation.EQUALS_OPER) {
                newV = currentValue;
            } else {
                int operationValue = 1;
                if (value != null) {
                    try {
                        operationValue = fmt.parse(value).intValue();
                    } catch (NumberFormatException nfe) {
                    } catch (ParseException pe)  {
                    }
                }
                if (operation == Operation.INCREMENT_OPER) {
                    newV = currentValue + operationValue;
                } else if (operation == Operation.DECREMENT_OPER) {
                    newV = currentValue - operationValue;
                }
            }
            this.newValue = fmt.format(newV);
        }
        private void executeString(String oldValue) throws BuildException {
            String newV  = DEFAULT_STRING_VALUE;
            String currentValue = getCurrentValue(oldValue);
            if (currentValue == null) {
                currentValue = DEFAULT_STRING_VALUE;
            }
            if (operation == Operation.EQUALS_OPER) {
                newV = currentValue;
            } else if (operation == Operation.INCREMENT_OPER) {
                newV = currentValue + value;
            }
            this.newValue = newV;
        }
        private void checkParameters() throws BuildException {
            if (type == Type.STRING_TYPE
                && operation == Operation.DECREMENT_OPER) {
                throw new BuildException("- is not supported for string "
                                         + "properties (key:" + key + ")");
            }
            if (value == null && defaultValue == null  && operation != Operation.DELETE_OPER) {
                throw new BuildException("\"value\" and/or \"default\" "
                                         + "attribute must be specified (key:" + key + ")");
            }
            if (key == null) {
                throw new BuildException("key is mandatory");
            }
            if (type == Type.STRING_TYPE && pattern != null) {
                throw new BuildException("pattern is not supported for string "
                                         + "properties (key:" + key + ")");
            }
        }
        private String getCurrentValue(String oldValue) {
            String ret = null;
            if (operation == Operation.EQUALS_OPER) {
                if (value != null && defaultValue == null) {
                    ret = value;
                }
                if (value == null && defaultValue != null && oldValue != null) {
                    ret = oldValue;
                }
                if (value == null && defaultValue != null && oldValue == null) {
                    ret = defaultValue;
                }
                if (value != null && defaultValue != null && oldValue != null) {
                    ret = value;
                }
                if (value != null && defaultValue != null && oldValue == null) {
                    ret = defaultValue;
                }
            } else {
                ret = (oldValue == null) ? defaultValue : oldValue;
            }
            return ret;
        }
        public static class Operation extends EnumeratedAttribute {
            public static final int INCREMENT_OPER =   0;
            public static final int DECREMENT_OPER =   1;
            public static final int EQUALS_OPER =      2;
            public static final int DELETE_OPER =      3;
            public String[] getValues() {
                return new String[] {"+", "-", "=", "del"};
            }
            public static int toOperation(String oper) {
                if ("+".equals(oper)) {
                    return INCREMENT_OPER;
                } else if ("-".equals(oper)) {
                    return DECREMENT_OPER;
                } else if ("del".equals(oper)) {
                    return DELETE_OPER;
                }
                return EQUALS_OPER;
            }
        }
        public static class Type extends EnumeratedAttribute {
            public static final int INTEGER_TYPE =     0;
            public static final int DATE_TYPE =        1;
            public static final int STRING_TYPE =      2;
            public String[] getValues() {
                return new String[] {"int", "date", "string"};
            }
            public static int toType(String type) {
                if ("int".equals(type)) {
                    return INTEGER_TYPE;
                } else if ("date".equals(type)) {
                    return DATE_TYPE;
                }
                return STRING_TYPE;
            }
        }
    }
    public static class Unit extends EnumeratedAttribute {
        private static final String MILLISECOND = "millisecond";
        private static final String SECOND = "second";
        private static final String MINUTE = "minute";
        private static final String HOUR = "hour";
        private static final String DAY = "day";
        private static final String WEEK = "week";
        private static final String MONTH = "month";
        private static final String YEAR = "year";
        private static final String[] UNITS
            = {MILLISECOND, SECOND, MINUTE, HOUR,
               DAY, WEEK, MONTH, YEAR };
        private Map calendarFields = new HashMap();
        public Unit() {
            calendarFields.put(MILLISECOND,
                               new Integer(Calendar.MILLISECOND));
            calendarFields.put(SECOND, new Integer(Calendar.SECOND));
            calendarFields.put(MINUTE, new Integer(Calendar.MINUTE));
            calendarFields.put(HOUR, new Integer(Calendar.HOUR_OF_DAY));
            calendarFields.put(DAY, new Integer(Calendar.DATE));
            calendarFields.put(WEEK, new Integer(Calendar.WEEK_OF_YEAR));
            calendarFields.put(MONTH, new Integer(Calendar.MONTH));
            calendarFields.put(YEAR, new Integer(Calendar.YEAR));
        }
        public int getCalendarField() {
            String key = getValue().toLowerCase();
            Integer i = (Integer) calendarFields.get(key);
            return i.intValue();
        }
        public String[] getValues() {
            return UNITS;
        }
    }
}