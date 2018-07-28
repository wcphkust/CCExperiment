package org.apache.log4j.helpers;
import java.io.Writer;
import java.io.IOException;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.ErrorCode;
public class CountingQuietWriter extends QuietWriter {
  protected long count;
  public
  CountingQuietWriter(Writer writer, ErrorHandler eh) {
    super(writer, eh);
  }
  public
  void write(String string) {
    try {
      out.write(string);
      count += string.length();
    }
    catch(IOException e) {
      errorHandler.error("Write failure.", e, ErrorCode.WRITE_FAILURE);
    }
  }
  public
  long getCount() {
    return count;
  }
  public
  void setCount(long count) {
    this.count = count;
  }
}