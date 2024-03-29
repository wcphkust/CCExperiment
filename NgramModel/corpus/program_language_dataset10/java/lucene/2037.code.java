package org.apache.lucene.util;
import org.apache.lucene.util.LuceneTestCase;
public class TestStringHelper extends LuceneTestCase {
  public void testStringDifference() {
    String test1 = "test";
    String test2 = "testing";
    int result = StringHelper.stringDifference(test1, test2);
    assertTrue(result == 4);
    test2 = "foo";
    result = StringHelper.stringDifference(test1, test2);
    assertTrue(result == 0);
    test2 = "test";
    result = StringHelper.stringDifference(test1, test2);
    assertTrue(result == 4);
  }
}
