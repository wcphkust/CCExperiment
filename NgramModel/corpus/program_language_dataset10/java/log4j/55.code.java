package org.apache.log4j;
public class BasicConfigurator {
  protected BasicConfigurator() {
  }
  static
  public
  void configure() {
    Logger root = Logger.getRootLogger();
    root.addAppender(new ConsoleAppender(
           new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
  }
  static
  public
  void configure(Appender appender) {
    Logger root = Logger.getRootLogger();
    root.addAppender(appender);
  }
  public
  static
  void resetConfiguration() {
    LogManager.resetConfiguration();
  }
}