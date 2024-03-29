package org.apache.lucene.benchmark.byTask.tasks;
import org.apache.lucene.benchmark.byTask.PerfRunData;
public class WaitTask extends PerfTask {
  private double waitTimeSec;
  public WaitTask(PerfRunData runData) {
    super(runData);
  }
  @Override
  public void setParams(String params) {
    super.setParams(params);
    if (params != null) {
      int multiplier;
      if (params.endsWith("s")) {
        multiplier = 1;
        params = params.substring(0, params.length()-1);
      } else if (params.endsWith("m")) {
        multiplier = 60;
        params = params.substring(0, params.length()-1);
      } else if (params.endsWith("h")) {
        multiplier = 3600;
        params = params.substring(0, params.length()-1);
      } else {
        multiplier = 1;
      }
      waitTimeSec = Double.parseDouble(params) * multiplier;
    } else {
      throw new IllegalArgumentException("you must specify the wait time, eg: 10.0s, 4.5m, 2h");
    }
  }
  @Override
  public int doLogic() throws Exception {
    Thread.sleep((long) (1000*waitTimeSec));
    return 0;
  }
  @Override
  public boolean supportsParams() {
    return true;
  }
}
