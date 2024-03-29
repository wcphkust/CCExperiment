package org.apache.log4j.helpers;
public class SingleLineTracerPrintWriter extends TracerPrintWriter {
  static String TAB = "    ";
  public SingleLineTracerPrintWriter(QuietWriter qWriter) {
    super(qWriter);
  }
  public
   void println(Object o) {
    this.qWriter.write(o.toString());
  }
  public
  void println(String s) {
      this.qWriter.write(TAB+s.substring(1));
  }
}
