package org.apache.tools.ant.util;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
public class ConcatResourceInputStream extends InputStream {
    private static final int EOF = -1;
    private boolean eof = false;
    private Iterator iter;
    private InputStream currentStream;
    private ProjectComponent managingPc;
    private boolean ignoreErrors = false;
    public ConcatResourceInputStream(ResourceCollection rc) {
        iter = rc.iterator();
    }
    public void setIgnoreErrors(boolean b) {
        ignoreErrors = b;
    }
    public boolean isIgnoreErrors() {
        return ignoreErrors;
    }
     public void close() throws IOException {
        closeCurrent();
        eof = true;
    }
    public int read() throws IOException {
        if (eof) {
            return EOF;
        }
        int result = readCurrent();
        if (result == EOF) {
            nextResource();
            result = readCurrent();
        }
        return result;
    }
    public void setManagingComponent(ProjectComponent pc) {
        this.managingPc = pc;
    }
    public void log(String message, int loglevel) {
        if (managingPc != null) {
            managingPc.log(message, loglevel);
        } else {
            (loglevel > Project.MSG_WARN ? System.out : System.err).println(message);
        }
    }
    private int readCurrent() throws IOException {
        return eof || currentStream == null ? EOF : currentStream.read();
    }
    private void nextResource() throws IOException {
        closeCurrent();
        while (iter.hasNext()) {
            Resource r = (Resource) iter.next();
            if (!r.isExists()) {
                continue;
            }
            log("Concating " + r.toLongString(), Project.MSG_VERBOSE);
            try {
                currentStream = new BufferedInputStream(r.getInputStream());
                return;
            } catch (IOException eyeOhEx) {
                if (!ignoreErrors) {
                    log("Failed to get input stream for " + r, Project.MSG_ERR);
                    throw eyeOhEx;
                }
            }
        }
        eof = true;
    }
    private void closeCurrent() {
        FileUtils.close(currentStream);
        currentStream = null;
    }
}