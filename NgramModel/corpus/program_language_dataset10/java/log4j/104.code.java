package org.apache.log4j.helpers;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.DateFormatSymbols;
public class DateTimeDateFormat extends AbsoluteTimeDateFormat {
  private static final long serialVersionUID = 5547637772208514971L;
  String[] shortMonths;
  public
  DateTimeDateFormat() {
    super();
    shortMonths = new DateFormatSymbols().getShortMonths();
  }
  public
  DateTimeDateFormat(TimeZone timeZone) {
    this();
    setCalendar(Calendar.getInstance(timeZone));
  }
  public
  StringBuffer format(Date date, StringBuffer sbuf,
		      FieldPosition fieldPosition) {
    calendar.setTime(date);
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    if(day < 10)
      sbuf.append('0');
    sbuf.append(day);
    sbuf.append(' ');
    sbuf.append(shortMonths[calendar.get(Calendar.MONTH)]);
    sbuf.append(' ');
    int year =  calendar.get(Calendar.YEAR);
    sbuf.append(year);
    sbuf.append(' ');
    return super.format(date, sbuf, fieldPosition);
  }
  public
  Date parse(java.lang.String s, ParsePosition pos) {
    return null;
  }
}
