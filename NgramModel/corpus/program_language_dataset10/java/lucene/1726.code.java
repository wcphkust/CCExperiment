package org.apache.lucene.search.function;
public class FieldScoreQuery extends ValueSourceQuery {
  public static class Type {
    public static final Type BYTE = new Type("byte"); 
    public static final Type SHORT = new Type("short"); 
    public static final Type INT = new Type("int"); 
    public static final Type FLOAT = new Type("float"); 
    private String typeName;
    private Type (String name) {
      this.typeName = name;
    }
    @Override
    public String toString() {
      return getClass().getName()+"::"+typeName;
    }
  }
  public FieldScoreQuery(String field, Type type) {
    super(getValueSource(field,type));
  }
  private static ValueSource getValueSource(String field, Type type) {
    if (type == Type.BYTE) {
      return new ByteFieldSource(field);
    }
    if (type == Type.SHORT) {
      return new ShortFieldSource(field);
    }
    if (type == Type.INT) {
      return new IntFieldSource(field);
    }
    if (type == Type.FLOAT) {
      return new FloatFieldSource(field);
    }
    throw new IllegalArgumentException(type+" is not a known Field Score Query Type!");
  }
}
