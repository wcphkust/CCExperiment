package org.apache.tools.ant.util;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.FileInputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.Task;
public class ConcatFileInputStream extends InputStream {
    private static final int EOF = -1;
    private int currentIndex = -1;
    private boolean eof = false;
    private File[] file;
    private InputStream currentStream;
    private ProjectComponent managingPc;
    public ConcatFileInputStream(File[] file) throws IOException {
        this.file = file;
    }
    public void close() throws IOException {
        closeCurrent();
        eof = true;
    }
    public int read() throws IOException {
        int result = readCurrent();
        if (result == EOF && !eof) {
            openFile(++currentIndex);
            result = readCurrent();
        }
        return result;
    }
    public void setManagingTask(Task task) {
        setManagingComponent(task);
    }
    public void setManagingComponent(ProjectComponent pc) {
        this.managingPc = pc;
    }
    public void log(String message, int loglevel) {
        if (managingPc != null) {
            managingPc.log(message, loglevel);
        } else {
            if (loglevel > Project.MSG_WARN) {
                System.out.println(message);
            } else {
                System.err.println(message);
            }
        }
    }
    private int readCurrent() throws IOException {
        return (eof || currentStream == null) ? EOF : currentStream.read();
    }
    private void openFile(int index) throws IOException {
        closeCurrent();
        if (file != null && index < file.length) {
            log("Opening " + file[index], Project.MSG_VERBOSE);
            try {
                currentStream = new BufferedInputStream(
                    new FileInputStream(file[index]));
            } catch (IOException eyeOhEx) {
                log("Failed to open " + file[index], Project.MSG_ERR);
                throw eyeOhEx;
            }
        } else {
            eof = true;
        }
    }
    private void closeCurrent() {
        FileUtils.close(currentStream);
        currentStream = null;
    }
}
