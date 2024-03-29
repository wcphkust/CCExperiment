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
public class AuthenticationRequest implements TBase<AuthenticationRequest, AuthenticationRequest._Fields>, java.io.Serializable, Cloneable {
  private static final TStruct STRUCT_DESC = new TStruct("AuthenticationRequest");
  private static final TField CREDENTIALS_FIELD_DESC = new TField("credentials", TType.MAP, (short)1);
  public Map<String,String> credentials;
  public enum _Fields implements TFieldIdEnum {
    CREDENTIALS((short)1, "credentials");
    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();
    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: 
          return CREDENTIALS;
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
    tmpMap.put(_Fields.CREDENTIALS, new FieldMetaData("credentials", TFieldRequirementType.REQUIRED, 
        new MapMetaData(TType.MAP, 
            new FieldValueMetaData(TType.STRING), 
            new FieldValueMetaData(TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    FieldMetaData.addStructMetaDataMap(AuthenticationRequest.class, metaDataMap);
  }
  public AuthenticationRequest() {
  }
  public AuthenticationRequest(
    Map<String,String> credentials)
  {
    this();
    this.credentials = credentials;
  }
  public AuthenticationRequest(AuthenticationRequest other) {
    if (other.isSetCredentials()) {
      Map<String,String> __this__credentials = new HashMap<String,String>();
      for (Map.Entry<String, String> other_element : other.credentials.entrySet()) {
        String other_element_key = other_element.getKey();
        String other_element_value = other_element.getValue();
        String __this__credentials_copy_key = other_element_key;
        String __this__credentials_copy_value = other_element_value;
        __this__credentials.put(__this__credentials_copy_key, __this__credentials_copy_value);
      }
      this.credentials = __this__credentials;
    }
  }
  public AuthenticationRequest deepCopy() {
    return new AuthenticationRequest(this);
  }
  @Override
  public void clear() {
    this.credentials = null;
  }
  public int getCredentialsSize() {
    return (this.credentials == null) ? 0 : this.credentials.size();
  }
  public void putToCredentials(String key, String val) {
    if (this.credentials == null) {
      this.credentials = new HashMap<String,String>();
    }
    this.credentials.put(key, val);
  }
  public Map<String,String> getCredentials() {
    return this.credentials;
  }
  public AuthenticationRequest setCredentials(Map<String,String> credentials) {
    this.credentials = credentials;
    return this;
  }
  public void unsetCredentials() {
    this.credentials = null;
  }
  public boolean isSetCredentials() {
    return this.credentials != null;
  }
  public void setCredentialsIsSet(boolean value) {
    if (!value) {
      this.credentials = null;
    }
  }
  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case CREDENTIALS:
      if (value == null) {
        unsetCredentials();
      } else {
        setCredentials((Map<String,String>)value);
      }
      break;
    }
  }
  public Object getFieldValue(_Fields field) {
    switch (field) {
    case CREDENTIALS:
      return getCredentials();
    }
    throw new IllegalStateException();
  }
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }
    switch (field) {
    case CREDENTIALS:
      return isSetCredentials();
    }
    throw new IllegalStateException();
  }
  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof AuthenticationRequest)
      return this.equals((AuthenticationRequest)that);
    return false;
  }
  public boolean equals(AuthenticationRequest that) {
    if (that == null)
      return false;
    boolean this_present_credentials = true && this.isSetCredentials();
    boolean that_present_credentials = true && that.isSetCredentials();
    if (this_present_credentials || that_present_credentials) {
      if (!(this_present_credentials && that_present_credentials))
        return false;
      if (!this.credentials.equals(that.credentials))
        return false;
    }
    return true;
  }
  @Override
  public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    boolean present_credentials = true && (isSetCredentials());
    builder.append(present_credentials);
    if (present_credentials)
      builder.append(credentials);
    return builder.toHashCode();
  }
  public int compareTo(AuthenticationRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }
    int lastComparison = 0;
    AuthenticationRequest typedOther = (AuthenticationRequest)other;
    lastComparison = Boolean.valueOf(isSetCredentials()).compareTo(typedOther.isSetCredentials());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetCredentials()) {
      lastComparison = TBaseHelper.compareTo(this.credentials, typedOther.credentials);
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
        case 1: 
          if (field.type == TType.MAP) {
            {
              TMap _map24 = iprot.readMapBegin();
              this.credentials = new HashMap<String,String>(2*_map24.size);
              for (int _i25 = 0; _i25 < _map24.size; ++_i25)
              {
                String _key26;
                String _val27;
                _key26 = iprot.readString();
                _val27 = iprot.readString();
                this.credentials.put(_key26, _val27);
              }
              iprot.readMapEnd();
            }
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
    if (this.credentials != null) {
      oprot.writeFieldBegin(CREDENTIALS_FIELD_DESC);
      {
        oprot.writeMapBegin(new TMap(TType.STRING, TType.STRING, this.credentials.size()));
        for (Map.Entry<String, String> _iter28 : this.credentials.entrySet())
        {
          oprot.writeString(_iter28.getKey());
          oprot.writeString(_iter28.getValue());
        }
        oprot.writeMapEnd();
      }
      oprot.writeFieldEnd();
    }
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("AuthenticationRequest(");
    boolean first = true;
    sb.append("credentials:");
    if (this.credentials == null) {
      sb.append("null");
    } else {
      sb.append(this.credentials);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }
  public void validate() throws TException {
    if (credentials == null) {
      throw new TProtocolException("Required field 'credentials' was not present! Struct: " + toString());
    }
  }
}
