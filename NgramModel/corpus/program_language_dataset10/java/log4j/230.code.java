package org.apache.log4j.spi;
import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
public interface LoggerRepository {
  public
  void addHierarchyEventListener(HierarchyEventListener listener);
  boolean isDisabled(int level);
  public
  void setThreshold(Level level);
  public
  void setThreshold(String val);
  public
  void emitNoAppenderWarning(Category cat);
  public
  Level getThreshold();
  public
  Logger getLogger(String name);
  public
  Logger getLogger(String name, LoggerFactory factory);
  public
  Logger getRootLogger();
  public
  abstract
  Logger exists(String name);
  public
  abstract
  void shutdown();
  public
  Enumeration getCurrentLoggers();
  public
  Enumeration getCurrentCategories();
  public
  abstract
  void fireAddAppenderEvent(Category logger, Appender appender);
  public
  abstract
  void resetConfiguration();
}
