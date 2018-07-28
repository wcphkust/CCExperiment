package org.apache.xml.dtm;
public interface DTMWSFilter
{
  public static final short NOTSTRIP = 1;
  public static final short STRIP = 2;
  public static final short INHERIT = 3;
  public short getShouldStripSpace(int elementHandle, DTM dtm);
}