package org.apache.tools.ant.taskdefs;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
public class Sleep extends Task {
    private boolean failOnError = true;
    private int seconds = 0;
    private int hours = 0;
    private int minutes = 0;
    private int milliseconds = 0;
    public Sleep() {
    }
    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }
    public void setHours(int hours) {
        this.hours = hours;
    }
    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }
    public void setMilliseconds(int milliseconds) {
        this.milliseconds = milliseconds;
    }
    public void doSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
        }
    }
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }
    private long getSleepTime() {
        return ((((long) hours * 60) + minutes) * 60 + seconds) * 1000
            + milliseconds;
    }
    public void validate()
        throws BuildException {
        if (getSleepTime() < 0) {
            throw new BuildException("Negative sleep periods are not "
                                     + "supported");
        }
    }
    public void execute()
        throws BuildException {
        try {
            validate();
            long sleepTime = getSleepTime();
            log("sleeping for " + sleepTime + " milliseconds",
                Project.MSG_VERBOSE);
            doSleep(sleepTime);
        } catch (Exception e) {
            if (failOnError) {
                throw new BuildException(e);
            } else {
                String text = e.toString();
                log(text, Project.MSG_ERR);
            }
        }
    }
}
