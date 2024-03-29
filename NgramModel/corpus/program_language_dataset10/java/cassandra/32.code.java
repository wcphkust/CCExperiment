package org.apache.cassandra.thrift;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.thrift.*;
import org.apache.thrift.async.*;
import org.apache.thrift.meta_data.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;
public class ColumnParent implements TBase<ColumnParent, ColumnParent._Fields>, java.io.Serializable, Cloneable {
  private static final TStruct STRUCT_DESC = new TStruct("ColumnParent");
  private static final TField COLUMN_FAMILY_FIELD_DESC = new TField("column_family", TType.STRING, (short)3);
  private static final TField SUPER_COLUMN_FIELD_DESC = new TField("super_column", TType.STRING, (short)4);
  public String column_family;
  public ByteBuffer super_column;
  public enum _Fields implements TFieldIdEnum {
    COLUMN_FAMILY((short)3, "column_family"),
    SUPER_COLUMN((short)4, "super_column");
    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 3: 
          return COLUMN_FAMILY;
        case 4: 
          return SUPER_COLUMN;
        default:
          return null;
      }
    }
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }
    public static _Fields findByName(String name) {
      return byName.get(name);
    }
    private final short _thriftId;
    private final String _fieldName;
    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }
    public short getThriftFieldId() {
      return _thriftId;
    }
    public String getFieldName() {
      return _fieldName;
    }
  }
  public static final Map<_Fields, FieldMetaData> metaDataMap;
  static {
    Map<_Fields, FieldMetaData> tmpMap = new EnumMap<_Fields, FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.COLUMN_FAMILY, new FieldMetaData("column_family", TFieldRequirementType.REQUIRED, 
        new FieldValueMetaData(TType.STRING)));
    tmpMap.put(_Fields.SUPER_COLUMN, new FieldMetaData("super_column", TFieldRequirementType.OPTIONAL, 
        new FieldValueMetaData(TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    FieldMetaData.addStructMetaDataMap(ColumnParent.class, metaDataMap);
  }
  public ColumnParent() {
  }
  public ColumnParent(
    String column_family)
  {
    this();
    this.column_family = column_family;
  }
  public ColumnParent(ColumnParent other) {
    if (other.isSetColumn_family()) {
      this.column_family = other.column_family;
    }
    if (other.isSetSuper_column()) {
      this.super_column = TBaseHelper.copyBinary(other.super_column);
;
    }
  }
  public ColumnParent deepCopy() {
    return new ColumnParent(this);
  }
  @Override
  public void clear() {
    this.column_family = null;
    this.super_column = null;
  }
  public String getColumn_family() {
    return this.column_family;
  }
  public ColumnParent setColumn_family(String column_family) {
    this.column_family = column_family;
    return this;
  }
  public void unsetColumn_family() {
    this.column_family = null;
  }
  public boolean isSetColumn_family() {
    return this.column_family != null;
  }
  public void setColumn_familyIsSet(boolean value) {
    if (!value) {
      this.column_family = null;
    }
  }
  public byte[] getSuper_column() {
    setSuper_column(TBaseHelper.rightSize(super_column));
    return super_column.array();
  }
  public ByteBuffer BufferForSuper_column() {
    return super_column;
  }
  public ColumnParent setSuper_column(byte[] super_column) {
    setSuper_column(ByteBuffer.wrap(super_column));
    return this;
  }
  public ColumnParent setSuper_column(ByteBuffer super_column) {
    this.super_column = super_column;
    return this;
  }
  public void unsetSuper_column() {
    this.super_column = null;
  }
  public boolean isSetSuper_column() {
    return this.super_column != null;
  }
  public void setSuper_columnIsSet(boolean value) {
    if (!value) {
      this.super_column = null;
    }
  }
  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case COLUMN_FAMILY:
      if (value == null) {
        unsetColumn_family();
      } else {
        setColumn_family((String)value);
      }
      break;
    case SUPER_COLUMN:
      if (value == null) {
        unsetSuper_column();
      } else {
        setSuper_column((ByteBuffer)value);
      }
      break;
    }
  }
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case COLUMN_FAMILY:
      return getColumn_family();
    case SUPER_COLUMN:
      return getSuper_column();
    }
    throw new IllegalStateException();
  }
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }
    switch (field) {
    case COLUMN_FAMILY:
      return isSetColumn_family();
    case SUPER_COLUMN:
      return isSetSuper_column();
    }
    throw new IllegalStateException();
  }
  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof ColumnParent)
      return this.equals((ColumnParent)that);
    return false;
  }
  public boolean equals(ColumnParent that) {
    if (that == null)
      return false;
    boolean this_present_column_family = true && this.isSetColumn_family();
    boolean that_present_column_family = true && that.isSetColumn_family();
    if (this_present_column_family || that_present_column_family) {
      if (!(this_present_column_family && that_present_column_family))
        return false;
      if (!this.column_family.equals(that.column_family))
        return false;
    }
    boolean this_present_super_column = true && this.isSetSuper_column();
    boolean that_present_super_column = true && that.isSetSuper_column();
    if (this_present_super_column || that_present_super_column) {
      if (!(this_present_super_column && that_present_super_column))
        return false;
      if (!this.super_column.equals(that.super_column))
        return false;
    }
    return true;
  }
  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    boolean present_column_family = true && (isSetColumn_family());
    builder.append(present_column_family);
    if (present_column_family)
      builder.append(column_family);
    boolean present_super_column = true && (isSetSuper_column());
    builder.append(present_super_column);
    if (present_super_column)
      builder.append(super_column);
    return builder.toHashCode();
  }
  public int compareTo(ColumnParent other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }
    int lastComparison = 0;
    ColumnParent typedOther = (ColumnParent)other;
    lastComparison = Boolean.valueOf(isSetColumn_family()).compareTo(typedOther.isSetColumn_family());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetColumn_family()) {
      lastComparison = TBaseHelper.compareTo(this.column_family, typedOther.column_family);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetSuper_column()).compareTo(typedOther.isSetSuper_column());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSuper_column()) {
      lastComparison = TBaseHelper.compareTo(this.super_column, typedOther.super_column);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }
  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }
  public void read(TProtocol iprot) throws TException {
    TField field;
    iprot.readStructBegin();
    while (true)
    {
      field = iprot.readFieldBegin();
      if (field.type == TType.STOP) { 
        break;
      }
      switch (field.id) {
        case 3: 
          if (field.type == TType.STRING) {
            this.column_family = iprot.readString();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        case 4: 
          if (field.type == TType.STRING) {
            this.super_column = iprot.readBinary();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        default:
          TProtocolUtil.skip(iprot, field.type);
      }
      iprot.readFieldEnd();
    }
    iprot.readStructEnd();
    validate();
  }
  public void write(TProtocol oprot) throws TException {
    validate();
    oprot.writeStructBegin(STRUCT_DESC);
    if (this.column_family != null) {
      oprot.writeFieldBegin(COLUMN_FAMILY_FIELD_DESC);
      oprot.writeString(this.column_family);
      oprot.writeFieldEnd();
    }
    if (this.super_column != null) {
      if (isSetSuper_column()) {
        oprot.writeFieldBegin(SUPER_COLUMN_FIELD_DESC);
        oprot.writeBinary(this.super_column);
        oprot.writeFieldEnd();
      }
    }
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ColumnParent(");
    boolean first = true;
    sb.append("column_family:");
    if (this.column_family == null) {
      sb.append("null");
    } else {
      sb.append(this.column_family);
    }
    first = false;
    if (isSetSuper_column()) {
      if (!first) sb.append(", ");
      sb.append("super_column:");
      if (this.super_column == null) {
        sb.append("null");
      } else {
        TBaseHelper.toString(this.super_column, sb);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }
  public void validate() throws TException {
    if (column_family == null) {
      throw new TProtocolException("Required field 'column_family' was not present! Struct: " + toString());
    }
  }
}
