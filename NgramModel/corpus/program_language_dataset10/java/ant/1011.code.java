package org.apache.tools.ant.taskdefs;
public class TimeProcess {
    public static void main(String[] args) throws Exception {
        int time = Integer.parseInt(args[0]);
        if (time < 1) {
            throw new IllegalArgumentException("Invalid time: " + time);
        }
        Thread.sleep(time);
    }
}
