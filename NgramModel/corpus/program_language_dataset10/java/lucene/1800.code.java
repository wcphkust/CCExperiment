package org.apache.lucene.util;
public abstract class MemoryModel {
  public abstract int getArraySize();
  public abstract int getClassSize();
  public abstract int getPrimitiveSize(Class<?> clazz);
  public abstract int getReferenceSize();
}
