package org.apache.log4j.pattern;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
public final class FileLocationPatternConverter
  extends LoggingEventPatternConverter {
  private static final FileLocationPatternConverter INSTANCE =
    new FileLocationPatternConverter();
  private FileLocationPatternConverter() {
    super("File Location", "file");
  }
  public static FileLocationPatternConverter newInstance(
    final String[] options) {
    return INSTANCE;
  }
  public void format(final LoggingEvent event, final StringBuffer output) {
    LocationInfo locationInfo = event.getLocationInformation();
    if (locationInfo != null) {
      output.append(locationInfo.getFileName());
    }
  }
}
