package org.apache.lucene.search.function;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldCache;
import java.io.IOException;
public class OrdFieldSource extends ValueSource {
  protected String field;
  public OrdFieldSource(String field) {
    this.field = field;
  }
  @Override
  public String description() {
    return "ord(" + field + ')';
  }
  @Override
  public DocValues getValues(IndexReader reader) throws IOException {
    final int[] arr = FieldCache.DEFAULT.getStringIndex(reader, field).order;
    return new DocValues() {
      @Override
      public float floatVal(int doc) {
        return arr[doc];
      }
      @Override
      public String strVal(int doc) {
        return Integer.toString(arr[doc]);
      }
      @Override
      public String toString(int doc) {
        return description() + '=' + intVal(doc);
      }
      @Override
      Object getInnerArray() {
        return arr;
      }
    };
  }
  @Override
  public boolean equals(Object o) {
    if (o.getClass() !=  OrdFieldSource.class) return false;
    OrdFieldSource other = (OrdFieldSource)o;
    return this.field.equals(other.field);
  }
  private static final int hcode = OrdFieldSource.class.hashCode();
  @Override
  public int hashCode() {
    return hcode + field.hashCode();
  }
}
