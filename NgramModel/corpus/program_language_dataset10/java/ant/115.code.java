package org.apache.tools.ant.filters;
import java.io.IOException;
import java.io.Reader;
import org.apache.tools.ant.types.Parameter;
public final class TabsToSpaces
    extends BaseParamFilterReader
    implements ChainableReader {
    private static final int DEFAULT_TAB_LENGTH = 8;
    private static final String TAB_LENGTH_KEY = "tablength";
    private int tabLength = DEFAULT_TAB_LENGTH;
    private int spacesRemaining = 0;
    public TabsToSpaces() {
        super();
    }
    public TabsToSpaces(final Reader in) {
        super(in);
    }
    public int read() throws IOException {
        if (!getInitialized()) {
            initialize();
            setInitialized(true);
        }
        int ch = -1;
        if (spacesRemaining > 0) {
            spacesRemaining--;
            ch = ' ';
        } else {
            ch = in.read();
            if (ch == '\t') {
                spacesRemaining = tabLength - 1;
                ch = ' ';
            }
        }
        return ch;
    }
    public void setTablength(final int tabLength) {
        this.tabLength = tabLength;
    }
    private int getTablength() {
        return tabLength;
    }
    public Reader chain(final Reader rdr) {
        TabsToSpaces newFilter = new TabsToSpaces(rdr);
        newFilter.setTablength(getTablength());
        newFilter.setInitialized(true);
        return newFilter;
    }
    private void initialize() {
        Parameter[] params = getParameters();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    if (TAB_LENGTH_KEY.equals(params[i].getName())) {
                        tabLength = Integer.parseInt(params[i].getValue());
                        break;
                    }
                }
            }
        }
    }
}
