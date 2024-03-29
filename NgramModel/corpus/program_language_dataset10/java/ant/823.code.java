package org.apache.tools.ant.util;
import java.util.Enumeration;
import java.util.Vector;
public class Watchdog implements Runnable {
    private Vector observers = new Vector(1);
    private long timeout = -1;
    private volatile boolean stopped = false;
    public static final String ERROR_INVALID_TIMEOUT = "timeout less than 1.";
    public Watchdog(long timeout) {
        if (timeout < 1) {
            throw new IllegalArgumentException(ERROR_INVALID_TIMEOUT);
        }
        this.timeout = timeout;
    }
    public void addTimeoutObserver(TimeoutObserver to) {
        observers.addElement(to);
    }
    public void removeTimeoutObserver(TimeoutObserver to) {
        observers.removeElement(to);
    }
    protected final void fireTimeoutOccured() {
        Enumeration e = observers.elements();
        while (e.hasMoreElements()) {
            ((TimeoutObserver) e.nextElement()).timeoutOccured(this);
        }
    }
    public synchronized void start() {
        stopped = false;
        Thread t = new Thread(this, "WATCHDOG");
        t.setDaemon(true);
        t.start();
    }
    public synchronized void stop() {
        stopped = true;
        notifyAll();
    }
    public synchronized void run() {
        long now = System.currentTimeMillis();
        final long until = now + timeout;
        try {
            while (!stopped && until > now) {
                wait(until - now);
                now = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
        }
        if (!stopped) {
            fireTimeoutOccured();
        }
    }
}
