package org.apache.xml.serializer;
import org.xml.sax.Attributes;
public interface SerializerTrace {
  public static final int EVENTTYPE_STARTDOCUMENT = 1;
  public static final int EVENTTYPE_ENDDOCUMENT = 2;
  public static final int EVENTTYPE_STARTELEMENT = 3;
  public static final int EVENTTYPE_ENDELEMENT = 4;
  public static final int EVENTTYPE_CHARACTERS = 5;
  public static final int EVENTTYPE_IGNORABLEWHITESPACE = 6;
  public static final int EVENTTYPE_PI = 7;
  public static final int EVENTTYPE_COMMENT = 8;
  public static final int EVENTTYPE_ENTITYREF = 9;
  public static final int EVENTTYPE_CDATA = 10;
  public static final int EVENTTYPE_OUTPUT_PSEUDO_CHARACTERS = 11;
  public static final int EVENTTYPE_OUTPUT_CHARACTERS = 12;
  public boolean hasTraceListeners();
  public void fireGenerateEvent(int eventType);
  public void fireGenerateEvent(int eventType, String name, Attributes atts);
  public void fireGenerateEvent(int eventType, char ch[], int start, int length);
  public void fireGenerateEvent(int eventType, String name, String data);
  public void fireGenerateEvent(int eventType, String data);
}
