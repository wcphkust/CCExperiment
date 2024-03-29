package org.apache.lucene.util;
public abstract class StringHelper {
  public static StringInterner interner = new SimpleStringInterner(1024,8);
  public static String intern(String s) {
    return interner.intern(s);
  }
  public static final int bytesDifference(byte[] bytes1, int len1, byte[] bytes2, int len2) {
    int len = len1 < len2 ? len1 : len2;
    for (int i = 0; i < len; i++)
      if (bytes1[i] != bytes2[i])
        return i;
    return len;
  }
  public static final int stringDifference(String s1, String s2) {
    int len1 = s1.length();
    int len2 = s2.length();
    int len = len1 < len2 ? len1 : len2;
    for (int i = 0; i < len; i++) {
      if (s1.charAt(i) != s2.charAt(i)) {
	      return i;
      }
    }
    return len;
  }
  private StringHelper() {
  }
}
