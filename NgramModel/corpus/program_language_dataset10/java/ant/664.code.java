package org.apache.tools.ant.types.resources;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
 class FailFast implements Iterator {
    private static final WeakHashMap MAP = new WeakHashMap();
    static synchronized void invalidate(Object o) {
        Set s = (Set) (MAP.get(o));
        if (s != null) {
            s.clear();
        }
    }
    private static synchronized void add(FailFast f) {
        Set s = (Set) (MAP.get(f.parent));
        if (s == null) {
            s = new HashSet();
            MAP.put(f.parent, s);
        }
        s.add(f);
    }
    private static synchronized void remove(FailFast f) {
        Set s = (Set) (MAP.get(f.parent));
        if (s != null) {
            s.remove(f);
        }
    }
    private static synchronized void failFast(FailFast f) {
        Set s = (Set) (MAP.get(f.parent));
        if (!s.contains(f)) {
            throw new ConcurrentModificationException();
        }
    }
    private Object parent;
    private Iterator wrapped;
    FailFast(Object o, Iterator i) {
        if (o == null) {
            throw new IllegalArgumentException("parent object is null");
        }
        if (i == null) {
            throw new IllegalArgumentException("cannot wrap null iterator");
        }
        parent = o;
        if (i.hasNext()) {
            wrapped = i;
            add(this);
        }
    }
    public boolean hasNext() {
        if (wrapped == null) {
            return false;
        }
        failFast(this);
        return wrapped.hasNext();
    }
    public Object next() {
        if (wrapped == null || !wrapped.hasNext()) {
            throw new NoSuchElementException();
        }
        failFast(this);
        try {
            return wrapped.next();
        } finally {
            if (!wrapped.hasNext()) {
                wrapped = null;
                remove(this);
            }
        }
    }
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
