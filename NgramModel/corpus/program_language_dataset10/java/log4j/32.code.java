package examples;
import org.apache.log4j.*;
import org.apache.log4j.helpers.PatternParser;
public class MyPatternLayout extends PatternLayout {
  public
  MyPatternLayout() {
    this(DEFAULT_CONVERSION_PATTERN);
  }
  public
  MyPatternLayout(String pattern) {
    super(pattern);
  }
  public
  PatternParser createPatternParser(String pattern) {
    return new MyPatternParser(
      pattern == null ? DEFAULT_CONVERSION_PATTERN : pattern);
  }
  public
  static void main(String[] args) {
    Layout layout = new MyPatternLayout("[counter=%.10#] - %m%n");
    Logger logger = Logger.getLogger("some.cat");
    logger.addAppender(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT));
    logger.debug("Hello, log");
    logger.info("Hello again...");    
  }
}
