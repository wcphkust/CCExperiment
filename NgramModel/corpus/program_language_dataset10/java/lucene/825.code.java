package org.apache.lucene.analysis.cn.smart.hhmm;
import java.util.Arrays;
class SegTokenPair {
  public char[] charArray;
  public int from;
  public int to;
  public double weight;
  public SegTokenPair(char[] idArray, int from, int to, double weight) {
    this.charArray = idArray;
    this.from = from;
    this.to = to;
    this.weight = weight;
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    for(int i=0;i<charArray.length;i++) {
      result = prime * result + charArray[i];
    }
    result = prime * result + from;
    result = prime * result + to;
    long temp;
    temp = Double.doubleToLongBits(weight);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SegTokenPair other = (SegTokenPair) obj;
    if (!Arrays.equals(charArray, other.charArray))
      return false;
    if (from != other.from)
      return false;
    if (to != other.to)
      return false;
    if (Double.doubleToLongBits(weight) != Double
        .doubleToLongBits(other.weight))
      return false;
    return true;
  }
}
