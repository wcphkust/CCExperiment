package org.apache.lucene.benchmark.byTask.tasks;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.benchmark.byTask.PerfRunData;
public class NewCollationAnalyzerTask extends PerfTask {
  public enum Implementation { 
    JDK("org.apache.lucene.collation.CollationKeyAnalyzer", 
        "java.text.Collator"),
    ICU("org.apache.lucene.collation.ICUCollationKeyAnalyzer", 
        "com.ibm.icu.text.Collator");
    String className;
    String collatorClassName;
    Implementation(String className, String collatorClassName) {
      this.className = className;
      this.collatorClassName = collatorClassName;
    }
  }
  private Implementation impl = Implementation.JDK;
  public NewCollationAnalyzerTask(PerfRunData runData) {
    super(runData);
  }
  static Analyzer createAnalyzer(Locale locale, Implementation impl)
      throws Exception {
    final Class<?> collatorClazz = Class.forName(impl.collatorClassName);
    Method collatorMethod = collatorClazz.getMethod("getInstance",
        new Class[] {Locale.class});
    Object collator = collatorMethod.invoke(null, locale);
    final Class<? extends Analyzer> clazz = Class.forName(impl.className)
        .asSubclass(Analyzer.class);
    Constructor<? extends Analyzer> ctor = clazz.getConstructor(collatorClazz);
    return ctor.newInstance(collator);
  }
  @Override
  public int doLogic() throws Exception {
    try {
      Locale locale = getRunData().getLocale();
      if (locale == null) throw new RuntimeException(
          "Locale must be set with the NewLocale task!");
      Analyzer analyzer = createAnalyzer(locale, impl);
      getRunData().setAnalyzer(analyzer);
      System.out.println("Changed Analyzer to: "
          + analyzer.getClass().getName() + "(" + locale + ")");
    } catch (Exception e) {
      throw new RuntimeException("Error creating Analyzer: impl=" + impl, e);
    }
    return 1;
  }
  @Override
  public void setParams(String params) {
    super.setParams(params);
    StringTokenizer st = new StringTokenizer(params, ",");
    while (st.hasMoreTokens()) {
      String param = st.nextToken();
      StringTokenizer expr = new StringTokenizer(param, ":");
      String key = expr.nextToken();
      String value = expr.nextToken();
      if (key.equals("impl")) {
        if (value.equalsIgnoreCase("icu"))
          impl = Implementation.ICU;
        else if (value.equalsIgnoreCase("jdk"))
          impl = Implementation.JDK;
        else
          throw new RuntimeException("Unknown parameter " + param);
      } else {
        throw new RuntimeException("Unknown parameter " + param);
      }
    }
  }
  @Override
  public boolean supportsParams() {
    return true;
  }
}
