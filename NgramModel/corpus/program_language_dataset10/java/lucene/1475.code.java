package org.apache.lucene.analysis.tokenattributes;
import java.io.Serializable;
import org.apache.lucene.util.AttributeImpl;
public class TypeAttributeImpl extends AttributeImpl implements TypeAttribute, Cloneable, Serializable {
  private String type;
  public TypeAttributeImpl() {
    this(DEFAULT_TYPE); 
  }
  public TypeAttributeImpl(String type) {
    this.type = type;
  }
  public String type() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  @Override
  public void clear() {
    type = DEFAULT_TYPE;    
  }
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other instanceof TypeAttributeImpl) {
      final TypeAttributeImpl o = (TypeAttributeImpl) other;
      return (this.type == null ? o.type == null : this.type.equals(o.type));
    }
    return false;
  }
  @Override
  public int hashCode() {
    return (type == null) ? 0 : type.hashCode();
  }
  @Override
  public void copyTo(AttributeImpl target) {
    TypeAttribute t = (TypeAttribute) target;
    t.setType(type);
  }
}
