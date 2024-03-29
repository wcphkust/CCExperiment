package org.apache.solr.handler.dataimport;
import org.apache.solr.core.SolrCore;
import static org.apache.solr.handler.dataimport.DataConfig.CLASS;
import static org.apache.solr.handler.dataimport.DataConfig.NAME;
import static org.apache.solr.handler.dataimport.DataImportHandlerException.SEVERE;
import static org.apache.solr.handler.dataimport.DataImportHandlerException.wrapAndThrow;
import static org.apache.solr.handler.dataimport.DocBuilder.loadClass;
import org.apache.solr.util.DateMathParser;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class EvaluatorBag {
  private static final Logger LOG = LoggerFactory.getLogger(EvaluatorBag.class);
  public static final String DATE_FORMAT_EVALUATOR = "formatDate";
  public static final String URL_ENCODE_EVALUATOR = "encodeUrl";
  public static final String ESCAPE_SOLR_QUERY_CHARS = "escapeQueryChars";
  public static final String SQL_ESCAPE_EVALUATOR = "escapeSql";
  static final Pattern FORMAT_METHOD = Pattern
          .compile("^(\\w*?)\\((.*?)\\)$");
  public static Evaluator getSqlEscapingEvaluator() {
    return new Evaluator() {
      public String evaluate(String expression, Context context) {
        List l = parseParams(expression, context.getVariableResolver());
        if (l.size() != 1) {
          throw new DataImportHandlerException(SEVERE, "'escapeSql' must have at least one parameter ");
        }
        String s = l.get(0).toString();
        return s.replaceAll("'", "''").replaceAll("\"", "\"\"").replaceAll("\\\\", "\\\\\\\\");
      }
    };
  }
  public static Evaluator getSolrQueryEscapingEvaluator() {
    return new Evaluator() {
      public String evaluate(String expression, Context context) {
        List l = parseParams(expression, context.getVariableResolver());
        if (l.size() != 1) {
          throw new DataImportHandlerException(SEVERE, "'escapeQueryChars' must have at least one parameter ");
        }
        String s = l.get(0).toString();
        return ClientUtils.escapeQueryChars(s);
      }
    };
  }
  public static Evaluator getUrlEvaluator() {
    return new Evaluator() {
      public String evaluate(String expression, Context context) {
        List l = parseParams(expression, context.getVariableResolver());
        if (l.size() != 1) {
          throw new DataImportHandlerException(SEVERE, "'encodeUrl' must have at least one parameter ");
        }
        String s = l.get(0).toString();
        try {
          return URLEncoder.encode(s.toString(), "UTF-8");
        } catch (Exception e) {
          wrapAndThrow(SEVERE, e, "Unable to encode expression: " + expression + " with value: " + s);
          return null;
        }
      }
    };
  }
  public static Evaluator getDateFormatEvaluator() {
    return new Evaluator() {
      public String evaluate(String expression, Context context) {
        List l = parseParams(expression, context.getVariableResolver());
        if (l.size() != 2) {
          throw new DataImportHandlerException(SEVERE, "'formatDate()' must have two parameters ");
        }
        Object o = l.get(0);
        Object format = l.get(1);
        if (format instanceof VariableWrapper) {
          VariableWrapper wrapper = (VariableWrapper) format;
          o = wrapper.resolve();
          if (o == null)  {
            format = wrapper.varName;
            LOG.warn("Deprecated syntax used. The syntax of formatDate has been changed to formatDate(<var>, '<date_format_string>'). " +
                    "The old syntax will stop working in Solr 1.5");
          } else  {
            format = o.toString();
          }
        }
        String dateFmt = format.toString();
        SimpleDateFormat fmt = new SimpleDateFormat(dateFmt);
        Date date = null;
        if (o instanceof VariableWrapper) {
          VariableWrapper variableWrapper = (VariableWrapper) o;
          Object variableval = variableWrapper.resolve();
          if (variableval instanceof Date) {
            date = (Date) variableval;
          } else {
            String s = variableval.toString();
            try {
              date = DataImporter.DATE_TIME_FORMAT.get().parse(s);
            } catch (ParseException exp) {
              wrapAndThrow(SEVERE, exp, "Invalid expression for date");
            }
          }
        } else {
          String datemathfmt = o.toString();
          datemathfmt = datemathfmt.replaceAll("NOW", "");
          try {
            date = dateMathParser.parseMath(datemathfmt);
          } catch (ParseException e) {
            wrapAndThrow(SEVERE, e, "Invalid expression for date");
          }
        }
        return fmt.format(date);
      }
    };
  }
  static Map<String, Object> getFunctionsNamespace(final List<Map<String, String>> fn, DocBuilder docBuilder) {
    final Map<String, Evaluator> evaluators = new HashMap<String, Evaluator>();
    evaluators.put(DATE_FORMAT_EVALUATOR, getDateFormatEvaluator());
    evaluators.put(SQL_ESCAPE_EVALUATOR, getSqlEscapingEvaluator());
    evaluators.put(URL_ENCODE_EVALUATOR, getUrlEvaluator());
    evaluators.put(ESCAPE_SOLR_QUERY_CHARS, getSolrQueryEscapingEvaluator());
    SolrCore core = docBuilder == null ? null : docBuilder.dataImporter.getCore();
    for (Map<String, String> map : fn) {
      try {
        evaluators.put(map.get(NAME), (Evaluator) loadClass(map.get(CLASS), core).newInstance());
      } catch (Exception e) {
        wrapAndThrow(SEVERE, e, "Unable to instantiate evaluator: " + map.get(CLASS));
      }
    }
    return new HashMap<String, Object>() {
      @Override
      public String get(Object key) {
        if (key == null)
          return null;
        Matcher m = FORMAT_METHOD.matcher((String) key);
        if (!m.find())
          return null;
        String fname = m.group(1);
        Evaluator evaluator = evaluators.get(fname);
        if (evaluator == null)
          return null;
        VariableResolverImpl vri = VariableResolverImpl.CURRENT_VARIABLE_RESOLVER.get();
        return evaluator.evaluate(m.group(2), Context.CURRENT_CONTEXT.get());
      }
    };
  }
  public static List parseParams(String expression, VariableResolver vr) {
    List result = new ArrayList();
    expression = expression.trim();
    String[] ss = expression.split(",");
    for (int i = 0; i < ss.length; i++) {
      ss[i] = ss[i].trim();
      if (ss[i].startsWith("'")) {
        StringBuilder sb = new StringBuilder();
        while (true) {
          sb.append(ss[i]);
          if (ss[i].endsWith("'")) break;
          i++;
          if (i >= ss.length)
            throw new DataImportHandlerException(SEVERE, "invalid string at " + ss[i - 1] + " in function params: " + expression);
          sb.append(",");
        }
        String s = sb.substring(1, sb.length() - 1);
        s = s.replaceAll("\\\\'", "'");
        result.add(s);
      } else {
        if (Character.isDigit(ss[i].charAt(0))) {
          try {
            Double doub = Double.parseDouble(ss[i]);
            result.add(doub);
          } catch (NumberFormatException e) {
            if (vr.resolve(ss[i]) == null) {
              wrapAndThrow(
                      SEVERE, e, "Invalid number :" + ss[i] +
                              "in parameters  " + expression);
            }
          }
        } else {
          result.add(new VariableWrapper(ss[i], vr));
        }
      }
    }
    return result;
  }
  public static class VariableWrapper {
    String varName;
    VariableResolver vr;
    public VariableWrapper(String s, VariableResolver vr) {
      this.varName = s;
      this.vr = vr;
    }
    public Object resolve() {
      return vr.resolve(varName);
    }
    public String toString() {
      Object o = vr.resolve(varName);
      return o == null ? null : o.toString();
    }
  }
  static Pattern IN_SINGLE_QUOTES = Pattern.compile("^'(.*?)'$");
  static DateMathParser dateMathParser = new DateMathParser(TimeZone
          .getDefault(), Locale.getDefault());
}
