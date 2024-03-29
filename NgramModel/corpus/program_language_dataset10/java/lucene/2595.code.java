package org.apache.solr.util;
import java.util.*;
public class BoundedTreeSet<E> extends TreeSet<E> {
  private int maxSize = Integer.MAX_VALUE;
  public BoundedTreeSet(int maxSize) {
    super();
    this.setMaxSize(maxSize);
  }
  public BoundedTreeSet(int maxSize, Collection<? extends E> c) {
    super(c);
    this.setMaxSize(maxSize);
  }
  public BoundedTreeSet(int maxSize, Comparator<? super E> c) {
    super(c);
    this.setMaxSize(maxSize);
  }
  public BoundedTreeSet(int maxSize, SortedSet<E> s) {
    super(s);
    this.setMaxSize(maxSize);
  }
  public int getMaxSize() {
    return maxSize;
  }
  public void setMaxSize(int max) {
    maxSize = max;
    adjust();
  }
  private void adjust() {
    while (maxSize < size()) {
      remove(last());
    }
  }
  public boolean add(E item) {
    boolean out = super.add(item);
    adjust();
    return out;
  }
  public boolean addAll(Collection<? extends E> c) {
    boolean out = super.addAll(c);
    adjust();
    return out;
  }
}
