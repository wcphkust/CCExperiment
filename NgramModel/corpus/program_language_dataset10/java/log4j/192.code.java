package org.apache.log4j.pattern;
import org.apache.log4j.spi.LoggingEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
public final class BridgePatternConverter
  extends org.apache.log4j.helpers.PatternConverter {
  private LoggingEventPatternConverter[] patternConverters;
  private FormattingInfo[] patternFields;
  private boolean handlesExceptions;
  public BridgePatternConverter(
    final String pattern) {
    next = null;
    handlesExceptions = false;
    List converters = new ArrayList();
    List fields = new ArrayList();
    Map converterRegistry = null;
    PatternParser.parse(
      pattern, converters, fields, converterRegistry,
      PatternParser.getPatternLayoutRules());
    patternConverters = new LoggingEventPatternConverter[converters.size()];
    patternFields = new FormattingInfo[converters.size()];
    int i = 0;
    Iterator converterIter = converters.iterator();
    Iterator fieldIter = fields.iterator();
    while (converterIter.hasNext()) {
      Object converter = converterIter.next();
      if (converter instanceof LoggingEventPatternConverter) {
        patternConverters[i] = (LoggingEventPatternConverter) converter;
        handlesExceptions |= patternConverters[i].handlesThrowable();
      } else {
        patternConverters[i] =
          new org.apache.log4j.pattern.LiteralPatternConverter("");
      }
      if (fieldIter.hasNext()) {
        patternFields[i] = (FormattingInfo) fieldIter.next();
      } else {
        patternFields[i] = FormattingInfo.getDefault();
      }
      i++;
    }
  }
  protected String convert(final LoggingEvent event) {
    StringBuffer sbuf = new StringBuffer();
    format(sbuf, event);
    return sbuf.toString();
  }
  public void format(final StringBuffer sbuf, final LoggingEvent e) {
    for (int i = 0; i < patternConverters.length; i++) {
      int startField = sbuf.length();
      patternConverters[i].format(e, sbuf);
      patternFields[i].format(startField, sbuf);
    }
  }
  public boolean ignoresThrowable() {
    return !handlesExceptions;
  }
}
