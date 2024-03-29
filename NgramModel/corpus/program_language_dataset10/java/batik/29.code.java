package org.apache.batik.anim.timing;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.batik.util.DoublyIndexedSet;
public abstract class TimedDocumentRoot extends TimeContainer {
    protected Calendar documentBeginTime;
    protected boolean useSVG11AccessKeys;
    protected boolean useSVG12AccessKeys;
    protected DoublyIndexedSet propagationFlags = new DoublyIndexedSet();
    protected LinkedList listeners = new LinkedList();
    protected boolean isSampling;
    protected boolean isHyperlinking;
    public TimedDocumentRoot(boolean useSVG11AccessKeys,
                             boolean useSVG12AccessKeys) {
        root = this;
        this.useSVG11AccessKeys = useSVG11AccessKeys;
        this.useSVG12AccessKeys = useSVG12AccessKeys;
    }
    protected float getImplicitDur() {
        return INDEFINITE;
    }
    public float getDefaultBegin(TimedElement child) {
        return 0.0f;
    }
    public float getCurrentTime() {
        return lastSampleTime;
    }
    public boolean isSampling() {
        return isSampling;
    }
    public boolean isHyperlinking() {
        return isHyperlinking;
    }
    public float seekTo(float time, boolean hyperlinking) {
        isSampling = true;
        lastSampleTime = time;
        isHyperlinking = hyperlinking;
        propagationFlags.clear();
        float mint = Float.POSITIVE_INFINITY;
        TimedElement[] es = getChildren();
        for (int i = 0; i < es.length; i++) {
            float t = es[i].sampleAt(time, hyperlinking);
            if (t < mint) {
                mint = t;
            }
        }
        boolean needsUpdates;
        do {
            needsUpdates = false;
            for (int i = 0; i < es.length; i++) {
                if (es[i].shouldUpdateCurrentInterval) {
                    needsUpdates = true;
                    float t = es[i].sampleAt(time, hyperlinking);
                    if (t < mint) {
                        mint = t;
                    }
                }
            }
        } while (needsUpdates);
        isSampling = false;
        if (hyperlinking) {
            root.currentIntervalWillUpdate();
        }
        return mint;
    }
    public void resetDocument(Calendar documentBeginTime) {
        if (documentBeginTime == null) {
            this.documentBeginTime = Calendar.getInstance();
        } else {
            this.documentBeginTime = documentBeginTime;
        }
        reset(true);
    }
    public Calendar getDocumentBeginTime() {
        return documentBeginTime;
    }
    public float convertEpochTime(long t) {
        long begin = documentBeginTime.getTime().getTime();
        return (t - begin) / 1000f;
    }
    public float convertWallclockTime(Calendar time) {
        long begin = documentBeginTime.getTime().getTime();
        long t = time.getTime().getTime();
        return (t - begin) / 1000f;
    }
    public void addTimegraphListener(TimegraphListener l) {
        listeners.add(l);
    }
    public void removeTimegraphListener(TimegraphListener l) {
        listeners.remove(l);
    }
    void fireElementAdded(TimedElement e) {
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            ((TimegraphListener) i.next()).elementAdded(e);
        }
    }
    void fireElementRemoved(TimedElement e) {
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            ((TimegraphListener) i.next()).elementRemoved(e);
        }
    }
    boolean shouldPropagate(Interval i, TimingSpecifier ts, boolean isBegin) {
        InstanceTime it = isBegin ? i.getBeginInstanceTime()
                                  : i.getEndInstanceTime();
        if (propagationFlags.contains(it, ts)) {
            return false;
        }
        propagationFlags.add(it, ts);
        return true;
    }
    protected void currentIntervalWillUpdate() {
    }
    protected abstract String getEventNamespaceURI(String eventName);
    protected abstract String getEventType(String eventName);
    protected abstract String getRepeatEventName();
}
