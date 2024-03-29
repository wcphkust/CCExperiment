package org.apache.cassandra.thrift;
import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;
public enum CqlResultType implements TEnum {
  ROWS(1),
  VOID(2),
  INT(3);
  private final int value;
  private CqlResultType(int value) {
    this.value = value;
  }
  public int getValue() {
    return value;
  }
  public static CqlResultType findByValue(int value) { 
    switch (value) {
      case 1:
        return ROWS;
      case 2:
        return VOID;
      case 3:
        return INT;
      default:
        return null;
    }
  }
}
