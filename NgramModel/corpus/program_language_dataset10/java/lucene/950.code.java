package org.apache.lucene.benchmark.byTask.tasks;
import org.apache.lucene.benchmark.byTask.PerfRunData;
public class CountingSearchTestTask extends SearchTask {
  public static int numSearches = 0; 
  public static long startMillis;
  public static long lastMillis;
  public static long prevLastMillis;
  public CountingSearchTestTask(PerfRunData runData) {
    super(runData);
  }
  @Override
  public int doLogic() throws Exception {
    int res = super.doLogic();
    incrNumSearches();
    return res;
  }
  private static synchronized void incrNumSearches() {
    prevLastMillis = lastMillis;
    lastMillis = System.currentTimeMillis();
    if (0 == numSearches) {
      startMillis = prevLastMillis = lastMillis;
    }
    numSearches++;
  }
  public long getElapsedMillis() {
    return lastMillis - startMillis;
  }
}
