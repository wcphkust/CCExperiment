package org.apache.lucene.analysis.sinks;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import org.apache.lucene.analysis.TeeSinkTokenFilter.SinkFilter;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;
public class DateRecognizerSinkFilter extends SinkFilter {
  public static final String DATE_TYPE = "date";
  protected DateFormat dateFormat;
  protected TermAttribute termAtt;
  public DateRecognizerSinkFilter() {
    this(DateFormat.getDateInstance());
  }
  public DateRecognizerSinkFilter(DateFormat dateFormat) {
    this.dateFormat = dateFormat; 
  }
  @Override
  public boolean accept(AttributeSource source) {
    if (termAtt == null) {
      termAtt = source.addAttribute(TermAttribute.class);
    }
    try {
      Date date = dateFormat.parse(termAtt.term());
      if (date != null) {
        return true;
      }
    } catch (ParseException e) {
    }
    return false;
  }
}
