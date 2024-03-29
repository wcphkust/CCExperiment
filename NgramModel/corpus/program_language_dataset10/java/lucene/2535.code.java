package org.apache.solr.search.function;
import org.apache.lucene.index.IndexReader;
import java.io.IOException;
public class PowFloatFunction extends DualFloatFunction {
  public PowFloatFunction(ValueSource a, ValueSource b) {
    super(a,b);
  }
  protected String name() {
    return "pow";
  }
  protected float func(int doc, DocValues aVals, DocValues bVals) {
    return (float)Math.pow(aVals.floatVal(doc), bVals.floatVal(doc));
  }
}
