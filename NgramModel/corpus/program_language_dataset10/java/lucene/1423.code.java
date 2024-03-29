package org.apache.lucene.analysis;
import java.util.ArrayList;
import java.util.List;
public abstract class BaseCharFilter extends CharFilter {
  private List<OffCorrectMap> pcmList;
  public BaseCharFilter(CharStream in) {
    super(in);
  }
  @Override
  protected int correct(int currentOff) {
    if (pcmList == null || pcmList.isEmpty()) {
      return currentOff;
    }
    for (int i = pcmList.size() - 1; i >= 0; i--) {
      if (currentOff >=  pcmList.get(i).off) {
        return currentOff + pcmList.get(i).cumulativeDiff;
      }
    }
    return currentOff;
  }
  protected int getLastCumulativeDiff() {
    return pcmList == null || pcmList.isEmpty() ?
      0 : pcmList.get(pcmList.size() - 1).cumulativeDiff;
  }
  protected void addOffCorrectMap(int off, int cumulativeDiff) {
    if (pcmList == null) {
      pcmList = new ArrayList<OffCorrectMap>();
    }
    pcmList.add(new OffCorrectMap(off, cumulativeDiff));
  }
  static class OffCorrectMap {
    int off;
    int cumulativeDiff;
    OffCorrectMap(int off, int cumulativeDiff) {
      this.off = off;
      this.cumulativeDiff = cumulativeDiff;
    }
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('(');
      sb.append(off);
      sb.append(',');
      sb.append(cumulativeDiff);
      sb.append(')');
      return sb.toString();
    }
  }
}
