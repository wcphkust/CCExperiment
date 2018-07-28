package org.apache.tools.ant.filters;
import java.io.IOException;
import java.io.Reader;
import org.apache.tools.ant.types.Parameter;
public final class PrefixLines
    extends BaseParamFilterReader
    implements ChainableReader {
    private static final String PREFIX_KEY = "prefix";
    private String prefix = null;
    private String queuedData = null;
    public PrefixLines() {
        super();
    }
    public PrefixLines(final Reader in) {
        super(in);
    }
    public int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }
        int ch = -1;
        if (queuedData != null && queuedData.length() == 0) {
            queuedData = null;
        }
        if (queuedData != null) {
            ch = queuedData.charAt(0);
            queuedData = queuedData.substring(1);
            if (queuedData.length() == 0) {
                queuedData = null;
            }
        } else {
            queuedData = readLine();
            if (queuedData == null) {
                ch = -1;
            } else {
                if (prefix != null) {
                    queuedData = prefix + queuedData;
                }
                return read();
            }
        }
        return ch;
    }
    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }
    private String getPrefix() {
        return prefix;
    }
    public Reader chain(final Reader rdr) {
        PrefixLines newFilter = new PrefixLines(rdr);
        newFilter.setPrefix(getPrefix());
        newFilter.setInitialized(true);
        return newFilter;
    }
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (PREFIX_KEY.equals(params[i].getName())) {
                    prefix = params[i].getValue();
                    break;
                }
            }
        }
    }
}