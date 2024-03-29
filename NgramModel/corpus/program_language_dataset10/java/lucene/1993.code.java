package org.apache.lucene.search.function;
import org.apache.lucene.util.LuceneTestCaseJ4;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
public class TestDocValues extends LuceneTestCaseJ4 {
  @Test
  public void testGetMinValue() {
    float[] innerArray = new float[] { 1.0f, 2.0f, -1.0f, 100.0f };
    DocValuesTestImpl docValues = new DocValuesTestImpl(innerArray);
    assertEquals("-1.0f is the min value in the source array", -1.0f, docValues
        .getMinValue(), 0);
    innerArray = new float[] {};
    docValues = new DocValuesTestImpl(innerArray);
    assertTrue("max is NaN - no values in inner array", Float.isNaN(docValues
        .getMinValue()));
  }
  @Test
  public void testGetMaxValue() {
    float[] innerArray = new float[] { 1.0f, 2.0f, -1.0f, 10.0f };
    DocValuesTestImpl docValues = new DocValuesTestImpl(innerArray);
    assertEquals("10.0f is the max value in the source array", 10.0f, docValues
        .getMaxValue(), 0);
    innerArray = new float[] { -3.0f, -1.0f, -100.0f };
    docValues = new DocValuesTestImpl(innerArray);
    assertEquals("-1.0f is the max value in the source array", -1.0f, docValues
        .getMaxValue(), 0);
    innerArray = new float[] { -3.0f, -1.0f, 100.0f, Float.MAX_VALUE,
        Float.MAX_VALUE - 1 };
    docValues = new DocValuesTestImpl(innerArray);
    assertEquals(Float.MAX_VALUE + " is the max value in the source array",
        Float.MAX_VALUE, docValues.getMaxValue(), 0);
    innerArray = new float[] {};
    docValues = new DocValuesTestImpl(innerArray);
    assertTrue("max is NaN - no values in inner array", Float.isNaN(docValues
        .getMaxValue()));
  }
  @Test
  public void testGetAverageValue() {
    float[] innerArray = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
    DocValuesTestImpl docValues = new DocValuesTestImpl(innerArray);
    assertEquals("the average is 1.0f", 1.0f, docValues.getAverageValue(), 0);
    innerArray = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f };
    docValues = new DocValuesTestImpl(innerArray);
    assertEquals("the average is 3.5f", 3.5f, docValues.getAverageValue(), 0);
    innerArray = new float[] { -1.0f, 2.0f };
    docValues = new DocValuesTestImpl(innerArray);
    assertEquals("the average is 0.5f", 0.5f, docValues.getAverageValue(), 0);
    innerArray = new float[] {};
    docValues = new DocValuesTestImpl(innerArray);
    assertTrue("the average is NaN - no values in inner array", Float
        .isNaN(docValues.getAverageValue()));
  }
  static class DocValuesTestImpl extends DocValues {
    float[] innerArray;
    DocValuesTestImpl(float[] innerArray) {
      this.innerArray = innerArray;
    }
    @Override
    public float floatVal(int doc) {
      return innerArray[doc];
    }
    @Override
    public String toString(int doc) {
      return Integer.toString(doc);
    }
  }
}
