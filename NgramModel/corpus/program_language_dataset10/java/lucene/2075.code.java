package org.apache.solr.handler.dataimport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DateFormatTransformer extends Transformer {
  private Map<String, SimpleDateFormat> fmtCache = new HashMap<String, SimpleDateFormat>();
  private static final Logger LOG = LoggerFactory
          .getLogger(DateFormatTransformer.class);
  @SuppressWarnings("unchecked")
  public Object transformRow(Map<String, Object> aRow, Context context) {
    for (Map<String, String> map : context.getAllEntityFields()) {
      Locale locale = Locale.getDefault();
      String customLocale = map.get("locale");
      if(customLocale != null){
        locale = new Locale(customLocale);
      }
      String fmt = map.get(DATE_TIME_FMT);
      if (fmt == null)
        continue;
      String column = map.get(DataImporter.COLUMN);
      String srcCol = map.get(RegexTransformer.SRC_COL_NAME);
      if (srcCol == null)
        srcCol = column;
      try {
        Object o = aRow.get(srcCol);
        if (o instanceof List) {
          List inputs = (List) o;
          List<Date> results = new ArrayList<Date>();
          for (Object input : inputs) {
            results.add(process(input, fmt, locale));
          }
          aRow.put(column, results);
        } else {
          if (o != null) {
            aRow.put(column, process(o, fmt, locale));
          }
        }
      } catch (ParseException e) {
        LOG.warn("Could not parse a Date field ", e);
      }
    }
    return aRow;
  }
  private Date process(Object value, String format, Locale locale) throws ParseException {
    if (value == null) return null;
    String strVal = value.toString().trim();
    if (strVal.length() == 0)
      return null;
    SimpleDateFormat fmt = fmtCache.get(format);
    if (fmt == null) {
      fmt = new SimpleDateFormat(format, locale);
      fmtCache.put(format, fmt);
    }
    return fmt.parse(strVal);
  }
  public static final String DATE_TIME_FMT = "dateTimeFormat";
}
