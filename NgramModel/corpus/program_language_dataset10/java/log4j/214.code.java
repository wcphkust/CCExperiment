package org.apache.log4j.pattern;
public abstract class PatternConverter {
  private final String name;
  private final String style;
  protected PatternConverter(final String name, final String style) {
    this.name = name;
    this.style = style;
  }
  public abstract void format(final Object obj, final StringBuffer toAppendTo);
  public final String getName() {
    return name;
  }
  public String getStyleClass(Object e) {
    return style;
  }
}
