package org.apache.lucene.queryParser.spans;
import org.apache.lucene.queryParser.core.nodes.FieldableNode;
import org.apache.lucene.util.Attribute;
public interface UniqueFieldAttribute extends Attribute {
  public void setUniqueField(CharSequence uniqueField);
  public CharSequence getUniqueField();
}
