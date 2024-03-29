package org.apache.lucene.analysis.sinks;
import java.io.IOException;
import org.apache.lucene.analysis.TeeSinkTokenFilter.SinkFilter;
import org.apache.lucene.util.AttributeSource;
public class TokenRangeSinkFilter extends SinkFilter {
  private int lower;
  private int upper;
  private int count;
  public TokenRangeSinkFilter(int lower, int upper) {
    this.lower = lower;
    this.upper = upper;
  }
  @Override
  public boolean accept(AttributeSource source) {
    try {
      if (count >= lower && count < upper){
        return true;
      }
      return false;
    } finally {
      count++;
    }
  }
  @Override
  public void reset() throws IOException {
    count = 0;
  }
}
